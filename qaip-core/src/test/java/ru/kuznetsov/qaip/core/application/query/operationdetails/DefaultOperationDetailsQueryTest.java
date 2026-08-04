package ru.kuznetsov.qaip.core.application.query.operationdetails;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.operationlist.OperationListProjector;
import ru.kuznetsov.qaip.core.domain.EvidenceManifest;
import ru.kuznetsov.qaip.core.domain.Metadata;
import ru.kuznetsov.qaip.core.domain.Node;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.domain.Relationship;
import ru.kuznetsov.qaip.core.domain.Subject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DefaultOperationDetailsQueryTest {
    @Test
    void returns_complete_direct_path_with_test_and_check_counts() {
        Project project = completeProject();

        OperationDetailsFound found = assertInstanceOf(
                OperationDetailsFound.class, query(project).execute("P-1", "OP-1"));

        assertEquals(new OperationDetailsResult(
                "OP-1", "POST", "/orders", "POST /orders", 1, 2,
                "OrdersController.create", "OrderService.create", "OrderRepository"), found.details());
    }

    @Test
    void distinguishes_operation_not_found_from_project_not_found() {
        assertEquals(new OperationDetailsOperationNotFound("P-1", "missing"),
                query(completeProject()).execute("P-1", "missing"));
        var missingProject = new DefaultOperationDetailsQuery(id -> Optional.empty(), new OperationListProjector());
        assertEquals(new OperationDetailsProjectNotFound(" missing-project "),
                missingProject.execute(" missing-project ", "OP-1"));
    }

    @Test
    void returns_typed_unavailable_for_incomplete_path() {
        Project complete = completeProject();
        Project incomplete = project(complete.nodes(), complete.relationships().stream()
                .filter(relationship -> !"SERVICE-USES-REPOSITORY".equals(relationship.id())).toList());

        assertEquals(new OperationDetailsUnavailable(
                        "P-1", "OP-1", OperationDetailsUnavailableReason.INCOMPLETE_PATH),
                query(incomplete).execute("P-1", "OP-1"));
    }

    @Test
    void returns_typed_unavailable_for_ambiguous_path() {
        Project complete = completeProject();
        List<Node> nodes = new ArrayList<>(complete.nodes());
        nodes.add(technical("SERVICE-2", "OtherOrderService.create", "SERVICE"));
        List<Relationship> relationships = new ArrayList<>(complete.relationships());
        relationships.add(relationship("CONTROLLER-USES-SERVICE-2", "CONTROLLER", "USES", "SERVICE-2"));

        assertEquals(new OperationDetailsUnavailable(
                        "P-1", "OP-1", OperationDetailsUnavailableReason.AMBIGUOUS_PATH),
                query(project(nodes, relationships)).execute("P-1", "OP-1"));
    }

    @Test
    void repeated_query_has_deterministic_value_equality() {
        OperationDetailsQuery query = query(completeProject());

        OperationDetailsQueryResult first = query.execute("P-1", "OP-1");
        OperationDetailsQueryResult second = query.execute("P-1", "OP-1");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    private static OperationDetailsQuery query(Project project) {
        return new DefaultOperationDetailsQuery(id -> Optional.of(project), new OperationListProjector());
    }

    private static Project completeProject() {
        List<Node> nodes = List.of(
                operation(),
                technical("CONTROLLER", "OrdersController.create", "CONTROLLER"),
                technical("SERVICE", "OrderService.create", "SERVICE"),
                technical("REPOSITORY", "OrderRepository", "REPOSITORY"),
                node("TEST-1", "TEST_IMPLEMENTATION"),
                node("CHECK-1", "CHECK"),
                node("CHECK-2", "CHECK"));
        List<Relationship> relationships = List.of(
                relationship("OP-IMPLEMENTS", "OP-1", "IMPLEMENTED_BY", "CONTROLLER"),
                relationship("CONTROLLER-USES-SERVICE", "CONTROLLER", "USES", "SERVICE"),
                relationship("SERVICE-USES-REPOSITORY", "SERVICE", "USES", "REPOSITORY"),
                relationship("TEST-USES-CONTROLLER", "TEST-1", "USES", "CONTROLLER"),
                relationship("TEST-CHECK-1", "TEST-1", "HAS_CHECK", "CHECK-1"),
                relationship("TEST-CHECK-2", "TEST-1", "HAS_CHECK", "CHECK-2"));
        return project(nodes, relationships);
    }

    private static Project project(List<Node> nodes, List<Relationship> relationships) {
        return new Project("contract", "schema", new Metadata("P-1", "Project", null, null, Map.of()),
                List.of(), new Subject("OP-1"), nodes, relationships,
                new EvidenceManifest("evidence", "source", Map.of(), "normalization", "canonicalization",
                        "fingerprint", List.of(), List.of(), List.of()), List.of(), Map.of());
    }

    private static Node operation() {
        return new Node("OP-1", "BUSINESS_OPERATION", "POST /orders", null, "CONFIRMED",
                List.of(), List.of(), Map.of(), Map.of("operation", Map.of("code", "OP-1")));
    }

    private static Node technical(String id, String name, String stage) {
        return new Node(id, "TECHNICAL_IMPLEMENTATION", name, null, "CONFIRMED", List.of(), List.of(), Map.of(),
                Map.of("technicalImplementation", Map.of(
                        "implementationType", "OTHER", "system", "orders", "details", Map.of("flowStage", stage))));
    }

    private static Node node(String id, String type) {
        return new Node(id, type, id, null, "CONFIRMED", List.of(), List.of(), Map.of(), Map.of());
    }

    private static Relationship relationship(String id, String from, String type, String to) {
        return new Relationship(id, from, type, to, Map.of(), List.of());
    }
}
