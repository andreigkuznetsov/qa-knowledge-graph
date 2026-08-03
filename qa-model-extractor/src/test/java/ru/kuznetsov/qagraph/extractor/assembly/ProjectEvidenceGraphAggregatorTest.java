package ru.kuznetsov.qagraph.extractor.assembly;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.extractor.rest.mapping.BusinessOperationProjection;
import ru.kuznetsov.qagraph.model.NodeType;
import ru.kuznetsov.qagraph.model.RelationshipType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectEvidenceGraphAggregatorTest {
    private final ProjectEvidenceGraphAggregator aggregator = new ProjectEvidenceGraphAggregator();

    @Test
    void aggregates_one_projection_without_changing_content() {
        EvidenceGraphProjection input = graph("BO-1", "GET /one", implementation("TI-1", "Controller.one"));

        var result = aggregator.aggregate(List.of(input));

        assertEquals(List.of(input.businessOperation()), result.businessOperations());
        assertEquals(input.technicalImplementations(), result.technicalImplementations());
        assertEquals(input.relationships(), result.relationships());
    }

    @Test
    void aggregates_multiple_operation_projections() {
        var result = aggregator.aggregate(List.of(
                graph("BO-2", "GET /two", implementation("TI-2", "Controller.two")),
                graph("BO-1", "GET /one", implementation("TI-1", "Controller.one"))));

        assertEquals(List.of("BO-1", "BO-2"), result.businessOperations().stream()
                .map(BusinessOperationProjection::id).toList());
        assertEquals(List.of("TI-1", "TI-2"), result.technicalImplementations().stream()
                .map(EvidenceGraphProjection.TechnicalImplementationProjection::id).toList());
        assertEquals(2, result.relationships().size());
    }

    @Test
    void deduplicates_shared_nodes_with_identical_content() {
        var shared = implementation("TI-SHARED", "SharedService.execute");

        var result = aggregator.aggregate(List.of(
                graph("BO-1", "GET /one", shared),
                graph("BO-2", "GET /two", shared)));

        assertEquals(List.of(shared), result.technicalImplementations());
        assertEquals("src/main/java/example/SharedService.java:10:5",
                result.technicalImplementations().getFirst().sourceReferences().getFirst().location().value());
    }

    @Test
    void deduplicates_identical_relationships() {
        var first = graphWithSharedVerification("BO-1", "GET /one");
        var second = graphWithSharedVerification("BO-2", "GET /two");

        var result = aggregator.aggregate(List.of(first, second));

        assertEquals(3, result.relationships().size());
        assertEquals(1, result.relationships().stream()
                .filter(relationship -> relationship.id().equals("REL-TEST-CHECK"))
                .count());
    }

    @Test
    void rejects_conflicting_content_for_the_same_node_identity() {
        var first = graph("BO-1", "GET /one", implementation("TI-SHARED", "FirstService.execute"));
        var second = graph("BO-2", "GET /two", implementation("TI-SHARED", "OtherService.execute"));

        var exception = assertThrows(IllegalArgumentException.class,
                () -> aggregator.aggregate(List.of(first, second)));

        assertEquals("Conflicting node content for identity TI-SHARED", exception.getMessage());
    }

    @Test
    void rejects_relationship_with_unknown_endpoint() {
        var operation = operation("BO-1", "GET /one");
        var invalid = new EvidenceGraphProjection(operation, List.of(), List.of(), List.of(), List.of(),
                List.of(relationship("REL-UNKNOWN", "BO-1", "UNKNOWN")));

        var exception = assertThrows(IllegalArgumentException.class,
                () -> aggregator.aggregate(List.of(invalid)));

        assertEquals("Relationship REL-UNKNOWN references an unknown endpoint", exception.getMessage());
    }

    @Test
    void sorts_every_node_category_and_relationship_by_identity() {
        var graph = new EvidenceGraphProjection(
                operation("BO-2", "GET /two"),
                List.of(rule("BR-2"), rule("BR-1")),
                List.of(implementation("TI-2", "Second"), implementation("TI-1", "First")),
                List.of(test("TEST-2"), test("TEST-1")),
                List.of(check("CHECK-2"), check("CHECK-1")),
                List.of(
                        relationship("REL-2", "TEST-2", "CHECK-2"),
                        relationship("REL-1", "TEST-1", "CHECK-1")));

        var result = aggregator.aggregate(List.of(graph));

        assertEquals(List.of("BR-1", "BR-2"), result.businessRules().stream()
                .map(EvidenceGraphProjection.BusinessRuleProjection::id).toList());
        assertEquals(List.of("TI-1", "TI-2"), result.technicalImplementations().stream()
                .map(EvidenceGraphProjection.TechnicalImplementationProjection::id).toList());
        assertEquals(List.of("TEST-1", "TEST-2"), result.testImplementations().stream()
                .map(EvidenceGraphProjection.TestImplementationProjection::id).toList());
        assertEquals(List.of("CHECK-1", "CHECK-2"), result.checks().stream()
                .map(EvidenceGraphProjection.CheckProjection::id).toList());
        assertEquals(List.of("REL-1", "REL-2"), result.relationships().stream()
                .map(EvidenceGraphProjection.RelationshipProjection::id).toList());
    }

    @Test
    void repeated_aggregation_is_equal_regardless_of_input_order() {
        var first = graph("BO-1", "GET /one", implementation("TI-1", "First"));
        var second = graph("BO-2", "GET /two", implementation("TI-2", "Second"));

        assertEquals(
                aggregator.aggregate(List.of(first, second)),
                aggregator.aggregate(List.of(second, first)));
    }

    @Test
    void aggregated_output_is_immutable_and_detached_from_input_collection() {
        var inputs = new ArrayList<>(List.of(
                graph("BO-1", "GET /one", implementation("TI-1", "First"))));
        var result = aggregator.aggregate(inputs);
        inputs.clear();

        assertEquals(1, result.businessOperations().size());
        assertThrows(UnsupportedOperationException.class,
                () -> result.businessOperations().add(operation("BO-2", "GET /two")));
        assertThrows(UnsupportedOperationException.class,
                () -> result.relationships().clear());
    }

    @Test
    void empty_input_produces_empty_valid_project_projection() {
        var result = aggregator.aggregate(List.of());

        assertTrue(result.businessOperations().isEmpty());
        assertTrue(result.businessRules().isEmpty());
        assertTrue(result.technicalImplementations().isEmpty());
        assertTrue(result.testImplementations().isEmpty());
        assertTrue(result.checks().isEmpty());
        assertTrue(result.relationships().isEmpty());
    }

    private static EvidenceGraphProjection graph(
            String operationId,
            String operationName,
            EvidenceGraphProjection.TechnicalImplementationProjection implementation
    ) {
        return new EvidenceGraphProjection(
                operation(operationId, operationName),
                List.of(),
                List.of(implementation),
                List.of(),
                List.of(),
                List.of(relationship("REL-" + operationId, operationId, implementation.id())));
    }

    private static EvidenceGraphProjection graphWithSharedVerification(String operationId, String operationName) {
        var test = test("TEST-SHARED");
        var check = check("CHECK-SHARED");
        return new EvidenceGraphProjection(
                operation(operationId, operationName),
                List.of(),
                List.of(),
                List.of(test),
                List.of(check),
                List.of(
                        relationship("REL-" + operationId + "-TEST", operationId, test.id()),
                        relationship("REL-TEST-CHECK", test.id(), check.id())));
    }

    private static BusinessOperationProjection operation(String id, String name) {
        return new BusinessOperationProjection(
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
                new BusinessOperationProjection.OperationProjection("OP-" + id, "example", null));
    }

    private static EvidenceGraphProjection.BusinessRuleProjection rule(String id) {
        return new EvidenceGraphProjection.BusinessRuleProjection(
                id,
                NodeType.BUSINESS_RULE,
                id,
                "Rule " + id,
                sourceReferences(),
                new EvidenceGraphProjection.RuleProjection(
                        "RULE-" + id, EvidenceGraphProjection.RuleType.VALIDATION_RULE, "Rule text", null));
    }

    private static EvidenceGraphProjection.TechnicalImplementationProjection implementation(
            String id, String name) {
        return new EvidenceGraphProjection.TechnicalImplementationProjection(
                id,
                NodeType.TECHNICAL_IMPLEMENTATION,
                name,
                "Implementation " + name,
                sourceReferences(),
                new EvidenceGraphProjection.TechnicalProjection(
                        EvidenceGraphProjection.ImplementationType.API, "example", Map.of()));
    }

    private static EvidenceGraphProjection.TestImplementationProjection test(String id) {
        return new EvidenceGraphProjection.TestImplementationProjection(
                id,
                NodeType.TEST_IMPLEMENTATION,
                id,
                "Test " + id,
                sourceReferences(),
                new EvidenceGraphProjection.TestProjection(
                        "CODE-" + id, EvidenceGraphProjection.ExecutionType.AUTOMATED, List.of(), List.of()));
    }

    private static EvidenceGraphProjection.CheckProjection check(String id) {
        return new EvidenceGraphProjection.CheckProjection(
                id,
                NodeType.CHECK,
                id,
                "Check " + id,
                sourceReferences(),
                new EvidenceGraphProjection.CheckContentProjection(
                        EvidenceGraphProjection.CheckType.API, "assertion", Map.of()));
    }

    private static List<EvidenceGraphProjection.SourceReferenceProjection> sourceReferences() {
        return List.of(new EvidenceGraphProjection.SourceReferenceProjection(
                "SOURCE",
                new EvidenceGraphProjection.SourceLocationProjection(
                        EvidenceGraphProjection.LocationType.OTHER,
                        "src/main/java/example/SharedService.java:10:5"),
                "Observed source",
                1.0,
                EvidenceGraphProjection.EvidenceType.OBSERVED));
    }

    private static EvidenceGraphProjection.RelationshipProjection relationship(
            String id, String from, String to) {
        return new EvidenceGraphProjection.RelationshipProjection(
                id, from, RelationshipType.IMPLEMENTED_BY, to);
    }
}
