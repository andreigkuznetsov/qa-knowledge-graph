package ru.kuznetsov.qagraph.extractor.implementationflow;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.extractor.assembly.OperationEvidenceAssemblyRequest;
import ru.kuznetsov.qagraph.extractor.assembly.OperationEvidenceGraphAssembler;
import ru.kuznetsov.qagraph.extractor.integrationtest.IntegrationTestEvidence;
import ru.kuznetsov.qagraph.extractor.rest.RestOperationEvidence;
import ru.kuznetsov.qagraph.extractor.rest.SpringMvcRestOperationScanner;
import ru.kuznetsov.qagraph.model.RelationshipType;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectImplementationFlowExtractorTest {
    private final SpringMvcRestOperationScanner scanner = new SpringMvcRestOperationScanner();
    private final DirectImplementationFlowExtractor extractor = new DirectImplementationFlowExtractor();

    @Test
    void resolvesConstructorInjectedServiceAndSpringDataRepository() throws Exception {
        ImplementationFlowEvidence flow = flow("/constructor").orElseThrow();

        assertEquals("example.controller.ConstructorController", flow.controllerClass());
        assertEquals("example.service.ConstructorService", flow.serviceClass());
        assertEquals("create", flow.serviceMethod());
        assertEquals("example.repository.UserRepository", flow.repositoryClass());
        assertEquals("save", flow.repositoryMethod());
        assertEquals(DependencyInjectionKind.CONSTRUCTOR, flow.serviceInjection());
        assertEquals(DependencyInjectionKind.CONSTRUCTOR, flow.repositoryInjection());
        assertEquals(ImplementationInvocationKind.DIRECT_FIELD_METHOD_INVOCATION, flow.invocationKind());
    }

    @Test
    void resolvesLombokConstructorInjection() throws Exception {
        ImplementationFlowEvidence flow = flow("/lombok").orElseThrow();

        assertEquals(DependencyInjectionKind.LOMBOK_CONSTRUCTOR, flow.serviceInjection());
        assertEquals(DependencyInjectionKind.LOMBOK_CONSTRUCTOR, flow.repositoryInjection());
        assertEquals("example.repository.UserRepository", flow.repositoryClass());
    }

    @Test
    void resolvesAutowiredFieldsAndExplicitRepositoryImplementation() throws Exception {
        ImplementationFlowEvidence flow = flow("/field").orElseThrow();

        assertEquals(DependencyInjectionKind.AUTOWIRED_FIELD, flow.serviceInjection());
        assertEquals(DependencyInjectionKind.AUTOWIRED_FIELD, flow.repositoryInjection());
        assertEquals("example.repository.ExplicitRepository", flow.repositoryClass());
        assertEquals("store", flow.repositoryMethod());
    }

    @Test
    void omitsMultipleRepositoriesServiceChainsAndDynamicDispatch() throws Exception {
        assertTrue(flow("/multiple").isEmpty());
        assertTrue(flow("/chain").isEmpty());
        assertTrue(flow("/dynamic").isEmpty());
    }

    @Test
    void isDeterministicLocationStableAndImmutable() throws Exception {
        List<ImplementationFlowEvidence> first = allFlows();
        List<ImplementationFlowEvidence> second = allFlows();

        assertEquals(first, second);
        assertEquals(3, first.size());
        assertEquals(first.size(), first.stream().distinct().count());
        assertEquals(List.of("/constructor", "/field", "/lombok"), first.stream()
                .map(value -> value.operation().endpointPath()).toList());
        assertTrue(first.stream().allMatch(value ->
                value.controllerInvocationLocation().repositoryRelativePath().startsWith("src/main/java/")
                        && value.serviceMethodLocation().line() > 0
                        && value.repositoryInvocationLocation().column() > 0
                        && value.repositoryDeclarationLocation().line() > 0));
        assertThrows(UnsupportedOperationException.class, () -> first.clear());
    }

    @Test
    void existingAssemblyAddsOrderedImplementationStagesWithoutNewRelationshipKinds() throws Exception {
        RestOperationEvidence operation = operation("/constructor");
        ImplementationFlowEvidence flow = extractor.extract(fixtureRepository(), operation).orElseThrow();
        var assembler = new OperationEvidenceGraphAssembler();
        var emptyTests = new IntegrationTestEvidence(List.of(), List.of(), List.of());
        var before = assembler.assemble(new OperationEvidenceAssemblyRequest(
                operation, List.of(), List.of(), emptyTests));
        var after = assembler.assemble(new OperationEvidenceAssemblyRequest(
                operation, List.of(), List.of(), emptyTests, List.of(flow)));

        assertEquals(1, before.technicalImplementations().size());
        assertEquals(3, after.technicalImplementations().size());
        assertEquals(List.of("CONTROLLER", "SERVICE", "REPOSITORY"), after.technicalImplementations().stream()
                .map(value -> value.technicalImplementation().details().getOrDefault("flowStage", "CONTROLLER"))
                .toList());
        assertEquals(1, after.relationships().stream()
                .filter(value -> value.type() == RelationshipType.IMPLEMENTED_BY).count());
        assertEquals(2, after.relationships().stream()
                .filter(value -> value.type() == RelationshipType.USES).count());
        assertEquals(Set.of(RelationshipType.IMPLEMENTED_BY, RelationshipType.USES),
                after.relationships().stream()
                .map(value -> value.type()).collect(java.util.stream.Collectors.toSet()));
        String controllerId = after.technicalImplementations().get(0).id();
        String serviceId = after.technicalImplementations().get(1).id();
        String repositoryId = after.technicalImplementations().get(2).id();
        assertTrue(after.relationships().stream().anyMatch(value ->
                value.from().equals(controllerId) && value.type() == RelationshipType.USES
                        && value.to().equals(serviceId)));
        assertTrue(after.relationships().stream().anyMatch(value ->
                value.from().equals(serviceId) && value.type() == RelationshipType.USES
                        && value.to().equals(repositoryId)));
    }

    @Test
    void rejectsNullInputs() throws Exception {
        assertThrows(NullPointerException.class, () -> extractor.extract(null, operation("/constructor")));
        assertThrows(NullPointerException.class, () -> extractor.extract(fixtureRepository(), null));
    }

    private Optional<ImplementationFlowEvidence> flow(String path) throws Exception {
        return extractor.extract(fixtureRepository(), operation(path));
    }

    private List<ImplementationFlowEvidence> allFlows() throws Exception {
        List<ImplementationFlowEvidence> result = new java.util.ArrayList<>();
        for (RestOperationEvidence operation : scanner.scan(fixtureRepository())) {
            extractor.extract(fixtureRepository(), operation).ifPresent(result::add);
        }
        return result.stream().sorted(java.util.Comparator.comparing(
                value -> value.operation().endpointPath())).toList();
    }

    private RestOperationEvidence operation(String path) throws Exception {
        return scanner.scan(fixtureRepository()).stream()
                .filter(value -> value.endpointPath().equals(path))
                .findFirst().orElseThrow();
    }

    private static Path fixtureRepository() throws URISyntaxException {
        return Path.of(DirectImplementationFlowExtractorTest.class.getResource(
                "/fixtures/implementation-flow-repository").toURI());
    }
}
