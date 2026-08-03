package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.kuznetsov.qagraph.extractor.assembly.EvidenceGraphProjection;
import ru.kuznetsov.qagraph.extractor.assembly.ProjectEvidenceGraphAggregator;
import ru.kuznetsov.qagraph.extractor.serialization.CanonicalProjectSerializer;
import ru.kuznetsov.qagraph.extractor.rest.mapping.BusinessOperationProjection;
import ru.kuznetsov.qagraph.model.NodeType;
import ru.kuznetsov.qagraph.model.RelationshipType;
import ru.kuznetsov.qaip.core.application.importing.DefaultProjectImporter;
import ru.kuznetsov.qaip.core.application.importing.ProjectImportSuccess;
import ru.kuznetsov.qaip.core.importing.binding.DefaultProjectBinder;
import ru.kuznetsov.qaip.core.importing.parsing.JacksonProjectJsonParser;
import ru.kuznetsov.qaip.core.importing.parsing.NetworkntProjectSchemaValidator;
import ru.kuznetsov.qaip.core.importing.parsing.RawProjectJson;
import ru.kuznetsov.qaip.core.validation.DefaultProjectApplicationValidator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultRepositoryAnalysisServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void produces_complete_canonical_repository_analysis() throws Exception {
        Path repository = repository("complete", controller("OneController", "/one", "one"));

        var result = new DefaultRepositoryAnalysisService().analyze(request(repository));

        assertEquals(RepositoryAnalysisStatus.COMPLETE, result.status());
        assertEquals(1, result.discoveredOperationCount());
        assertTrue(result.warnings().isEmpty());
        assertEquals(Optional.empty(), result.failureMessage());
        assertNotNull(result.projectIdentity());
        assertEquals(result.projectIdentity(),
                result.canonicalProjectJson().at("/baseModel/project/id").textValue());
        assertEquals(1, result.canonicalProjectJson().at("/baseModel/nodes").findValues("operation").size());
        assertInstanceOf(ProjectImportSuccess.class, canonicalImporter().importProject(
                new RawProjectJson(result.canonicalProjectJson().toString())));
    }

    @Test
    void produces_partial_result_when_discovery_has_warnings() {
        EvidenceGraphProjection operation = operationGraph("BO-1", "GET /one");
        var service = service(
                request -> new RepositoryEvidenceDiscoveryResult(
                        List.of(operation), List.of("Bounded construct was omitted")),
                new ProjectEvidenceGraphAggregator()::aggregate,
                new CanonicalProjectSerializer()::serializeProject);

        var result = service.analyze(new RepositoryAnalysisRequest(Path.of("unused"), "Example"));

        assertEquals(RepositoryAnalysisStatus.PARTIAL, result.status());
        assertEquals(List.of("Bounded construct was omitted"), result.warnings());
        assertNotNull(result.canonicalProjectJson());
    }

    @Test
    void returns_failed_result_when_discovery_fails() {
        var service = service(
                request -> { throw new IllegalStateException("discovery"); },
                new ProjectEvidenceGraphAggregator()::aggregate,
                new CanonicalProjectSerializer()::serializeProject);

        var result = service.analyze(new RepositoryAnalysisRequest(Path.of("unused"), "Example"));

        assertFailed(result, 0, "Repository evidence discovery failed");
    }

    @Test
    void returns_failed_result_when_aggregation_fails_and_preserves_discovered_count() {
        var operation = operationGraph("BO-1", "GET /one");
        var service = service(
                request -> new RepositoryEvidenceDiscoveryResult(List.of(operation), List.of()),
                projections -> { throw new IllegalArgumentException("aggregation"); },
                new CanonicalProjectSerializer()::serializeProject);

        var result = service.analyze(new RepositoryAnalysisRequest(Path.of("unused"), "Example"));

        assertFailed(result, 1, "Project evidence aggregation failed");
    }

    @Test
    void returns_failed_result_when_serialization_fails_and_preserves_discovered_count() {
        var operation = operationGraph("BO-1", "GET /one");
        var service = service(
                request -> new RepositoryEvidenceDiscoveryResult(List.of(operation), List.of()),
                new ProjectEvidenceGraphAggregator()::aggregate,
                (graph, metadata) -> { throw new IllegalStateException("serialization"); });

        var result = service.analyze(new RepositoryAnalysisRequest(Path.of("unused"), "Example"));

        assertFailed(result, 1, "Canonical project serialization failed");
    }

    @Test
    void zero_operation_repository_is_an_explicit_failure() throws Exception {
        Path repository = repository("empty", "package example; class PlainService { }");

        var result = new DefaultRepositoryAnalysisService().analyze(request(repository));

        assertFailed(result, 0, "Repository analysis discovered no supported operations");
    }

    @Test
    void repeated_analysis_is_deterministically_equal() throws Exception {
        Path repository = repository("repeat", controller("OneController", "/one", "one"));
        var service = new DefaultRepositoryAnalysisService();

        var first = service.analyze(request(repository));
        var second = service.analyze(request(repository));

        assertEquals(first, second);
    }

    @Test
    void project_identity_is_stable_across_absolute_repository_locations() throws Exception {
        String source = controller("OneController", "/one", "one");
        Path firstRepository = repository("location-one", source);
        Path secondRepository = repository("location-two", source);
        var service = new DefaultRepositoryAnalysisService();

        var first = service.analyze(request(firstRepository));
        var second = service.analyze(request(secondRepository));

        assertEquals(first.projectIdentity(), second.projectIdentity());
        assertEquals(first.canonicalProjectJson(), second.canonicalProjectJson());
    }

    @Test
    void reports_every_discovered_operation() throws Exception {
        Path repository = repository("multiple", """
                package example;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.PostMapping;
                import org.springframework.web.bind.annotation.RestController;
                @RestController
                class MultiController {
                    @GetMapping("/one") String one() { return "one"; }
                    @PostMapping("/two") String two() { return "two"; }
                }
                """);

        var result = new DefaultRepositoryAnalysisService().analyze(request(repository));

        assertEquals(RepositoryAnalysisStatus.COMPLETE, result.status());
        assertEquals(2, result.discoveredOperationCount());
        assertEquals(2, result.canonicalProjectJson().at("/baseModel/nodes").findValues("operation").size());
    }

    @Test
    void canonical_result_contains_no_absolute_repository_path() throws Exception {
        Path repository = repository("no-path", controller("OneController", "/one", "one"));

        var result = new DefaultRepositoryAnalysisService().analyze(request(repository));
        String canonical = result.canonicalProjectJson().toString();
        String absolute = repository.toAbsolutePath().toString();

        assertFalse(canonical.contains(absolute));
        assertFalse(canonical.contains(absolute.replace('\\', '/')));
        assertFalse(result.projectIdentity().contains(absolute));
    }

    private static DefaultRepositoryAnalysisService service(
            DefaultRepositoryAnalysisService.DiscoveryStep discovery,
            DefaultRepositoryAnalysisService.AggregationStep aggregation,
            DefaultRepositoryAnalysisService.SerializationStep serialization
    ) {
        return new DefaultRepositoryAnalysisService(discovery, aggregation, serialization);
    }

    private static DefaultProjectImporter canonicalImporter() {
        return new DefaultProjectImporter(
                new JacksonProjectJsonParser(),
                new NetworkntProjectSchemaValidator(),
                new DefaultProjectBinder(),
                new DefaultProjectApplicationValidator());
    }

    private RepositoryAnalysisRequest request(Path repository) {
        return new RepositoryAnalysisRequest(repository, "Example project");
    }

    private Path repository(String name, String source) throws Exception {
        Path repository = temporaryDirectory.resolve(name);
        Path file = repository.resolve("src/main/java/example/Controller.java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, source);
        return repository;
    }

    private static void assertFailed(
            RepositoryAnalysisResult result, int operationCount, String failureMessage) {
        assertEquals(RepositoryAnalysisStatus.FAILED, result.status());
        assertEquals(operationCount, result.discoveredOperationCount());
        assertEquals(Optional.of(failureMessage), result.failureMessage());
        assertNull(result.projectIdentity());
        assertNull(result.canonicalProjectJson());
    }

    private static String controller(String className, String path, String method) {
        return """
                package example;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RestController;
                @RestController
                class %s {
                    @GetMapping("%s") String %s() { return "value"; }
                }
                """.formatted(className, path, method);
    }

    private static EvidenceGraphProjection operationGraph(String id, String name) {
        var operation = new BusinessOperationProjection(
                id,
                NodeType.BUSINESS_OPERATION,
                name,
                "Operation " + name,
                List.of(new BusinessOperationProjection.SourceReferenceProjection(
                        "SOURCE",
                        new BusinessOperationProjection.SourceReferenceLocation(
                                BusinessOperationProjection.LocationType.OTHER,
                                "src/main/java/example/Controller.java:5:5"),
                        "Controller operation",
                        1.0,
                        BusinessOperationProjection.EvidenceType.OBSERVED)),
                new BusinessOperationProjection.OperationProjection("OP-1", "example", null));
        var implementation = new EvidenceGraphProjection.TechnicalImplementationProjection(
                "TI-1",
                NodeType.TECHNICAL_IMPLEMENTATION,
                "Controller.one",
                "Controller implementation",
                List.of(new EvidenceGraphProjection.SourceReferenceProjection(
                        "SOURCE",
                        new EvidenceGraphProjection.SourceLocationProjection(
                                EvidenceGraphProjection.LocationType.OTHER,
                                "src/main/java/example/Controller.java:5:5"),
                        "Controller implementation",
                        1.0,
                        EvidenceGraphProjection.EvidenceType.OBSERVED)),
                new EvidenceGraphProjection.TechnicalProjection(
                        EvidenceGraphProjection.ImplementationType.API, "example", Map.of()));
        return new EvidenceGraphProjection(
                operation,
                List.of(),
                List.of(implementation),
                List.of(),
                List.of(),
                List.of(new EvidenceGraphProjection.RelationshipProjection(
                        "REL-1", id, RelationshipType.IMPLEMENTED_BY, implementation.id())));
    }
}
