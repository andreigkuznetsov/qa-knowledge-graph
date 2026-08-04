package ru.kuznetsov.qaip.core.application.query.operationlist;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.domain.EvidenceManifest;
import ru.kuznetsov.qaip.core.domain.Metadata;
import ru.kuznetsov.qaip.core.domain.Node;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.domain.Relationship;
import ru.kuznetsov.qaip.core.domain.Subject;
import ru.kuznetsov.qaip.core.persistence.read.ProjectReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultOperationListQueryTest {
    private final OperationListProjector projector = new OperationListProjector();

    @Test
    void returns_one_operation_with_qualified_test_and_check_counts() {
        Project project = project(
                List.of(operation("OP-1", "POST /orders", "ignored"), node("TI-1", "TECHNICAL_IMPLEMENTATION"),
                        node("TEST-1", "TEST_IMPLEMENTATION"), node("TEST-2", "TEST_IMPLEMENTATION"),
                        node("CHECK-1", "CHECK"), node("CHECK-2", "CHECK")),
                List.of(relationship("OP-1", "IMPLEMENTED_BY", "TI-1"),
                        relationship("TEST-1", "USES", "TI-1"), relationship("TEST-2", "USES", "TI-1"),
                        relationship("TEST-1", "HAS_CHECK", "CHECK-1"),
                        relationship("TEST-2", "HAS_CHECK", "CHECK-2")));

        OperationListFound found = assertInstanceOf(OperationListFound.class, query(project).execute("P-1"));

        assertEquals(List.of(new OperationQueryResult("OP-1", "POST", "/orders", "POST /orders", 2, 2)),
                found.operations());
    }

    @Test
    void returns_multiple_operations_in_method_path_and_id_order() {
        Project project = project(List.of(
                operation("OP-3", "POST /orders", "ignored"),
                operation("OP-2", "GET /z", "ignored"),
                operation("OP-1", "GET /a", "ignored"),
                operation("OP-0", "GET /a", "ignored"),
                node("BR-1", "BUSINESS_RULE")), List.of());

        OperationListFound found = assertInstanceOf(OperationListFound.class, query(project).execute("P-1"));

        assertEquals(List.of("OP-0", "OP-1", "OP-2", "OP-3"),
                found.operations().stream().map(OperationQueryResult::operationId).toList());
    }

    @Test
    void counts_tests_and_checks_through_scenario_evidence_without_duplicates() {
        Project project = project(
                List.of(operation("OP-1", "GET /orders", "ignored"), node("SC-1", "SCENARIO"),
                        node("TEST-1", "TEST_IMPLEMENTATION"), node("CHECK-1", "CHECK")),
                List.of(relationship("OP-1", "SPECIFIED_BY", "SC-1"),
                        relationship("TEST-1", "VALIDATES", "SC-1"),
                        relationship("TEST-1", "VALIDATES", "SC-1"),
                        relationship("TEST-1", "HAS_CHECK", "CHECK-1")));

        OperationListFound found = assertInstanceOf(OperationListFound.class, query(project).execute("P-1"));

        assertEquals(1, found.operations().getFirst().testCount());
        assertEquals(1, found.operations().getFirst().checkCount());
    }

    @Test
    void distinguishes_project_with_no_operations_from_unknown_project() {
        OperationListFound empty = assertInstanceOf(
                OperationListFound.class, query(project(List.of(node("BR-1", "BUSINESS_RULE")), List.of()))
                        .execute("P-1"));
        assertEquals(List.of(), empty.operations());

        OperationListQuery missing = new DefaultOperationListQuery(id -> Optional.empty(), projector);
        assertEquals(new OperationListProjectNotFound(" missing "), missing.execute(" missing "));
    }

    @Test
    void result_is_immutable_and_repeated_queries_have_value_equality() {
        Project project = project(List.of(operation("OP-1", "GET /orders", "ignored")), List.of());
        OperationListFound first = assertInstanceOf(OperationListFound.class, query(project).execute("P-1"));
        OperationListFound second = assertInstanceOf(OperationListFound.class, query(project).execute("P-1"));

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertThrows(UnsupportedOperationException.class,
                () -> first.operations().add(new OperationQueryResult("X", "GET", "/x", "GET /x", 0, 0)));

        List<OperationQueryResult> source = new ArrayList<>(first.operations());
        OperationListFound copied = new OperationListFound(source);
        source.clear();
        assertEquals(first, copied);
    }

    private OperationListQuery query(Project project) {
        ProjectReader reader = id -> Optional.of(project);
        return new DefaultOperationListQuery(reader, projector);
    }

    private static Project project(List<Node> nodes, List<Relationship> relationships) {
        return new Project("contract", "schema", new Metadata("P-1", "Project", null, null, Map.of()),
                List.of(), new Subject("OP-1"), nodes, relationships,
                new EvidenceManifest("evidence", "source", Map.of(), "normalization", "canonicalization",
                        "fingerprint", List.of(), List.of(), List.of()), List.of(), Map.of());
    }

    private static Node operation(String id, String name, String description) {
        return new Node(id, "BUSINESS_OPERATION", name, description, "CONFIRMED",
                List.of(), List.of(), Map.of(), Map.of("operation", Map.of("code", id)));
    }

    private static Node node(String id, String type) {
        return new Node(id, type, id, null, "CONFIRMED", List.of(), List.of(), Map.of(), Map.of());
    }

    private static Relationship relationship(String from, String type, String to) {
        return new Relationship(from + '-' + type + '-' + to, from, type, to, Map.of(), List.of());
    }
}
