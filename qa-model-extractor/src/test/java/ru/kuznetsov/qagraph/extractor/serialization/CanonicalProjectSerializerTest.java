package ru.kuznetsov.qagraph.extractor.serialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.extractor.assembly.EvidenceGraphProjection;
import ru.kuznetsov.qagraph.extractor.rest.mapping.BusinessOperationProjection;
import ru.kuznetsov.qagraph.model.NodeType;
import ru.kuznetsov.qagraph.model.RelationshipType;
import ru.kuznetsov.qaip.core.application.importing.DefaultProjectImporter;
import ru.kuznetsov.qaip.core.application.importing.ProjectImportSuccess;
import ru.kuznetsov.qaip.core.importing.binding.DefaultProjectBinder;
import ru.kuznetsov.qaip.core.importing.parsing.JacksonProjectJsonParser;
import ru.kuznetsov.qaip.core.importing.parsing.NetworkntProjectSchemaValidator;
import ru.kuznetsov.qaip.core.importing.parsing.RawProjectJson;
import ru.kuznetsov.qaip.core.validation.ApplicationValidProjectDocument;
import ru.kuznetsov.qaip.core.validation.DefaultProjectApplicationValidator;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalProjectSerializerTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final CanonicalProjectSerializer serializer = new CanonicalProjectSerializer();

    @Test
    void serializes_graph_through_schema_binding_and_application_validation() throws Exception {
        EvidenceGraphProjection graph = graph();
        ProjectSerializationMetadata metadata = metadata();

        byte[] json = serializer.serialize(graph, metadata);
        var importer = new DefaultProjectImporter(
                new JacksonProjectJsonParser(), new NetworkntProjectSchemaValidator(),
                new DefaultProjectBinder(), new DefaultProjectApplicationValidator());
        ProjectImportSuccess success = assertInstanceOf(ProjectImportSuccess.class,
                importer.importProject(new RawProjectJson(new String(json, StandardCharsets.UTF_8))));
        ApplicationValidProjectDocument document = assertInstanceOf(
                ApplicationValidProjectDocument.class, success.document());

        assertTrue(success.warnings().isEmpty());
        assertEquals("PROJECT-REGISTRATION", document.project().metadata().id());
        assertEquals("Registration Evidence", document.project().metadata().name());
        assertEquals(7, document.project().nodes().size());
        assertEquals(6, document.project().relationships().size());
        assertEquals(nodeIds(graph), document.project().nodes().stream()
                .map(node -> node.id()).collect(Collectors.toSet()));
        assertEquals(triples(graph), document.project().relationships().stream()
                .map(value -> triple(value.from(), value.type(), value.to())).collect(Collectors.toSet()));

        JsonNode root = JSON.readTree(json);
        assertEquals("qaip-project-v1", root.get("projectContractVersion").textValue());
        assertEquals("0.1", root.at("/baseModel/schemaVersion").textValue());
        assertEquals(1, root.at("/baseModel/sources").size());
        assertEquals("SOURCE-REPOSITORY", root.at("/baseModel/sources/0/id").textValue());
        assertEquals("abc123", root.at("/baseModel/sources/0/version").textValue());
        assertEquals("BO-REST-1", root.at("/subject/localArtifactId").textValue());
        assertEquals(1, root.get("declaredChanges").size());
        assertFalse(new String(json, StandardCharsets.UTF_8).contains("USER_STORY"));
        assertFalse(new String(json, StandardCharsets.UTF_8).contains("SCENARIO"));
        root.at("/baseModel/nodes").forEach(node -> node.get("sourceReferences").forEach(reference -> {
            assertEquals("SOURCE-REPOSITORY", reference.get("sourceId").textValue());
            assertFalse(reference.at("/location/value").textValue().matches("^[A-Za-z]:.*"));
        }));
    }

    @Test
    void serialization_is_byte_identical_sorted_and_does_not_mutate_input() throws Exception {
        EvidenceGraphProjection graph = graph();
        EvidenceGraphProjection unchanged = graph();

        byte[] first = serializer.serialize(graph, metadata());
        byte[] second = serializer.serialize(graph, metadata());

        assertArrayEquals(first, second);
        assertEquals(unchanged, graph);
        JsonNode root = JSON.readTree(first);
        List<String> nodeIds = root.at("/baseModel/nodes").findValuesAsText("id");
        assertEquals(nodeIds.stream().sorted().toList(), nodeIds);
        List<String> relationshipIds = root.at("/baseModel/relationships").findValuesAsText("id");
        assertEquals(relationshipIds.stream().sorted().toList(), relationshipIds);
    }

    @Test
    void rejects_null_graph_metadata_and_incomplete_metadata() {
        assertThrows(NullPointerException.class, () -> serializer.serialize(null, metadata()));
        assertThrows(NullPointerException.class, () -> serializer.serialize(graph(), null));
        assertThrows(IllegalArgumentException.class, () -> new ProjectSerializationMetadata(
                "", "Registration Evidence", null, null, Map.of(), source(), "BO-REST-1", evidence()));
        var unknownSubject = new ProjectSerializationMetadata(
                "PROJECT-REGISTRATION", "Registration Evidence", null, null, Map.of(),
                source(), "UNKNOWN", evidence());
        assertThrows(IllegalArgumentException.class, () -> serializer.serialize(graph(), unknownSubject));
        assertThrows(IllegalArgumentException.class, () -> new ProjectSerializationMetadata.RepositorySource(
                "SOURCE-REPOSITORY", "Fixture repository", "", "example/fixture",
                "https://example.test/fixture", "abc123", Map.of()));
    }

    private static EvidenceGraphProjection graph() {
        var operation = new BusinessOperationProjection(
                "BO-REST-1", NodeType.BUSINESS_OPERATION, "POST /auth/register",
                "Spring MVC operation POST /auth/register.",
                List.of(new BusinessOperationProjection.SourceReferenceProjection(
                        "ORIGINAL-CONTROLLER-SOURCE",
                        new BusinessOperationProjection.SourceReferenceLocation(
                                BusinessOperationProjection.LocationType.OTHER,
                                "src/main/java/example/AuthController.java:20:5#register"),
                        "Controller method", 1.0, BusinessOperationProjection.EvidenceType.OBSERVED)),
                new BusinessOperationProjection.OperationProjection(
                        "OP-POST-AUTH-REGISTER", "example.api", null));
        var source = List.of(new EvidenceGraphProjection.SourceReferenceProjection(
                "ORIGINAL-EVIDENCE-SOURCE",
                new EvidenceGraphProjection.SourceLocationProjection(
                        EvidenceGraphProjection.LocationType.OTHER,
                        "src/main/java/example/RegisterRequest.java:8:5#email"),
                "Declared annotation", 1.0, EvidenceGraphProjection.EvidenceType.OBSERVED));
        var testSource = List.of(new EvidenceGraphProjection.SourceReferenceProjection(
                "ORIGINAL-TEST-SOURCE",
                new EvidenceGraphProjection.SourceLocationProjection(
                        EvidenceGraphProjection.LocationType.TEST_CASE,
                        "src/test/java/example/RegistrationIT.java:14:5#registers"),
                "JUnit test", 1.0, EvidenceGraphProjection.EvidenceType.OBSERVED));
        var ruleOne = new EvidenceGraphProjection.BusinessRuleProjection(
                "BR-2", NodeType.BUSINESS_RULE, "Email is required", "@NotBlank on email.", source,
                new EvidenceGraphProjection.RuleProjection(
                        "VALIDATION-2", EvidenceGraphProjection.RuleType.VALIDATION_RULE,
                        "@NotBlank is declared on email.", null));
        var ruleTwo = new EvidenceGraphProjection.BusinessRuleProjection(
                "BR-1", NodeType.BUSINESS_RULE, "Email has valid format", "@Email on email.", source,
                new EvidenceGraphProjection.RuleProjection(
                        "VALIDATION-1", EvidenceGraphProjection.RuleType.VALIDATION_RULE,
                        "@Email is declared on email.", null));
        var implementation = new EvidenceGraphProjection.TechnicalImplementationProjection(
                "TI-1", NodeType.TECHNICAL_IMPLEMENTATION, "AuthController.register", "Controller method.",
                source, new EvidenceGraphProjection.TechnicalProjection(
                        EvidenceGraphProjection.ImplementationType.API, "example.api", Map.of("endpoint", "POST /auth/register")));
        var test = new EvidenceGraphProjection.TestImplementationProjection(
                "TEST-1", NodeType.TEST_IMPLEMENTATION, "RegistrationIT.registers", "Automated test.",
                testSource, new EvidenceGraphProjection.TestProjection(
                        "TEST-CODE-1", EvidenceGraphProjection.ExecutionType.AUTOMATED, List.of(), List.of()));
        var apiCheck = new EvidenceGraphProjection.CheckProjection(
                "CHECK-2", NodeType.CHECK, "HTTP_STATUS assertion", "Status assertion.", testSource,
                new EvidenceGraphProjection.CheckContentProjection(
                        EvidenceGraphProjection.CheckType.API, "statusCode(201)", Map.of("category", "HTTP_STATUS")));
        var databaseCheck = new EvidenceGraphProjection.CheckProjection(
                "CHECK-1", NodeType.CHECK, "PERSISTENCE_DATABASE assertion", "Database assertion.", testSource,
                new EvidenceGraphProjection.CheckContentProjection(
                        EvidenceGraphProjection.CheckType.SQL, "assertUserStored()", Map.of("category", "PERSISTENCE_DATABASE")));
        return new EvidenceGraphProjection(operation, List.of(ruleOne, ruleTwo), List.of(implementation),
                List.of(test), List.of(apiCheck, databaseCheck), List.of(
                relationship("REL-6", "TEST-1", RelationshipType.HAS_CHECK, "CHECK-2"),
                relationship("REL-1", "BO-REST-1", RelationshipType.IMPLEMENTED_BY, "TI-1"),
                relationship("REL-4", "TEST-1", RelationshipType.USES, "TI-1"),
                relationship("REL-3", "BO-REST-1", RelationshipType.GOVERNED_BY, "BR-2"),
                relationship("REL-5", "TEST-1", RelationshipType.HAS_CHECK, "CHECK-1"),
                relationship("REL-2", "BO-REST-1", RelationshipType.GOVERNED_BY, "BR-1")));
    }

    private static EvidenceGraphProjection.RelationshipProjection relationship(
            String id, String from, RelationshipType type, String to) {
        return new EvidenceGraphProjection.RelationshipProjection(id, from, type, to);
    }

    private static ProjectSerializationMetadata metadata() {
        return new ProjectSerializationMetadata(
                "PROJECT-REGISTRATION", "Registration Evidence", "Fixture evidence graph", "1",
                Map.of("repositoryRevision", "abc123"), source(), "BO-REST-1", evidence());
    }

    private static ProjectSerializationMetadata.RepositorySource source() {
        return new ProjectSerializationMetadata.RepositorySource(
                "SOURCE-REPOSITORY", "Fixture repository", "abc123", "example/fixture",
                "https://example.test/fixture", "abc123", Map.of());
    }

    private static ProjectSerializationMetadata.EvidenceMetadata evidence() {
        return new ProjectSerializationMetadata.EvidenceMetadata(
                "abc123", "sha256:content", "sha256:manifest", "PROV-REPOSITORY",
                "https://example.test/fixture/tree/abc123", "sha256:origin",
                "Deterministic extraction and evidence graph assembly");
    }

    private static Set<String> nodeIds(EvidenceGraphProjection graph) {
        return Set.of("BO-REST-1", "BR-1", "BR-2", "TI-1", "TEST-1", "CHECK-1", "CHECK-2");
    }

    private static Set<String> triples(EvidenceGraphProjection graph) {
        return graph.relationships().stream()
                .map(value -> triple(value.from(), value.type().name(), value.to())).collect(Collectors.toSet());
    }

    private static String triple(String from, String type, String to) {
        return from + '\0' + type + '\0' + to;
    }
}
