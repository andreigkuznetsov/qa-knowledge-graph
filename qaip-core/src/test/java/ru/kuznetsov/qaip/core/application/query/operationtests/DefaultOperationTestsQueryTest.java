package ru.kuznetsov.qaip.core.application.query.operationtests;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.eventpath.DefaultEventPathQuery;
import ru.kuznetsov.qaip.core.application.query.operationlist.DefaultOperationListQuery;
import ru.kuznetsov.qaip.core.application.query.operationlist.OperationListProjector;
import ru.kuznetsov.qaip.core.domain.EvidenceManifest;
import ru.kuznetsov.qaip.core.domain.Metadata;
import ru.kuznetsov.qaip.core.domain.Node;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.domain.Relationship;
import ru.kuznetsov.qaip.core.domain.Subject;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DefaultOperationTestsQueryTest {
    private final OperationListProjector qualificationProjector = new OperationListProjector();

    @Test
    void returns_checks_attached_to_their_qualified_tests() {
        Project project = qualifiedProject();

        OperationTestsFound found = assertInstanceOf(
                OperationTestsFound.class, query(project).execute("P", "OP"));

        assertEquals(2, found.tests().size());
        assertEquals(List.of("TEST-1", "TEST-2"), found.tests().stream()
                .map(QualifiedOperationTest::testId).toList());
        assertEquals(List.of("example.FirstIT", "example.SecondIT"), found.tests().stream()
                .map(QualifiedOperationTest::testClass).toList());
        assertEquals(List.of("createsOrder", "rejectsOrder"), found.tests().stream()
                .map(QualifiedOperationTest::testMethod).toList());
        assertEquals(List.of("CHECK-1", "CHECK-2"), found.tests().getFirst().checks().stream()
                .map(QualifiedOperationCheck::checkId).toList());
        assertEquals(List.of("CHECK-3"), found.tests().get(1).checks().stream()
                .map(QualifiedOperationCheck::checkId).toList());
        assertEquals(2, found.tests().getFirst().qualifiedCheckCount());
        assertEquals(1, found.tests().get(1).qualifiedCheckCount());
        assertEquals(3, found.checkCount());
        assertEquals(OperationVerificationStatus.VERIFIED, found.verificationStatus());
    }

    @Test
    void qualified_test_without_checks_remains_valid() {
        Node test = test("TEST-1", "example.FirstIT.createsOrder", null);
        Project project = project(List.of(operation(), implementation(), test), List.of(
                relationship("OP-IMPL", "OP", "IMPLEMENTED_BY", "IMPL"),
                relationship("T1-USES", "TEST-1", "USES", "IMPL")));

        OperationTestsFound found = assertInstanceOf(
                OperationTestsFound.class, query(project).execute("P", "OP"));

        assertEquals(List.of(), found.tests().getFirst().checks());
        assertEquals(0, found.checkCount());
        assertEquals(OperationVerificationStatus.PARTIALLY_VERIFIED, found.verificationStatus());
    }

    @Test
    void returns_project_not_found_operation_not_found_and_no_qualified_tests() {
        assertEquals(new OperationTestsProjectNotFound("missing"),
                new DefaultOperationTestsQuery(id -> Optional.empty(), qualificationProjector)
                        .execute("missing", "OP"));
        Project project = project(List.of(operation(), node("OTHER", "BUSINESS_OPERATION", "GET /other")),
                List.of());
        assertEquals(new OperationTestsOperationNotFound("P", "missing"),
                query(project).execute("P", "missing"));
        assertEquals(new OperationTestsNoneQualified("P", "OP"), query(project).execute("P", "OP"));
    }

    @Test
    void ordering_and_duplicate_elimination_are_deterministic() {
        Project project = qualifiedProject();

        OperationTestsFound first = assertInstanceOf(OperationTestsFound.class, query(project).execute("P", "OP"));
        OperationTestsFound second = assertInstanceOf(OperationTestsFound.class, query(project).execute("P", "OP"));

        assertEquals(first, second);
        assertEquals(2, first.tests().size());
        assertEquals(List.of("TEST-1", "TEST-2"), first.tests().stream()
                .map(QualifiedOperationTest::testId).toList());
        assertEquals(List.of("CHECK-1", "CHECK-2"), first.tests().getFirst().checks().stream()
                .map(QualifiedOperationCheck::checkId).toList());
    }

    @Test
    void names_packages_and_unrelated_graph_proximity_do_not_qualify_tests() {
        Node conventionMatch = test("TEST-NAME", "example.OrderApiIT.createsOrder", null);
        Project project = project(List.of(operation(), implementation(), conventionMatch),
                List.of(relationship("OP-IMPL", "OP", "IMPLEMENTED_BY", "IMPL")));

        assertInstanceOf(OperationTestsNoneQualified.class, query(project).execute("P", "OP"));
    }

    @Test
    void existing_runtime_queries_retain_the_shared_qualification_counts() {
        Project project = qualifiedProject();
        var operationList = new DefaultOperationListQuery(id -> Optional.of(project), qualificationProjector);

        assertEquals(2, assertInstanceOf(
                ru.kuznetsov.qaip.core.application.query.operationlist.OperationListFound.class,
                operationList.execute("P")).operations().getFirst().testCount());
        assertEquals(3, assertInstanceOf(
                ru.kuznetsov.qaip.core.application.query.operationlist.OperationListFound.class,
                operationList.execute("P")).operations().getFirst().checkCount());
        assertInstanceOf(ru.kuznetsov.qaip.core.application.query.eventpath.EventPathIncomplete.class,
                new DefaultEventPathQuery(id -> Optional.of(project)).execute("P", "OP"));
    }

    private OperationTestsQuery query(Project project) {
        return new DefaultOperationTestsQuery(id -> Optional.of(project), qualificationProjector);
    }

    private static Project qualifiedProject() {
        Node second = test("TEST-2", "display second", "JUnit 5 test method example.SecondIT.rejectsOrder.");
        Node first = test("TEST-1", "display first", "JUnit 5 test method example.FirstIT.createsOrder.");
        Node unrelated = check("CHECK-X", OperationCheckType.API);
        Relationship firstUse = relationship("T1-USES", "TEST-1", "USES", "IMPL");
        Relationship firstCheck = relationship("T1-C1", "TEST-1", "HAS_CHECK", "CHECK-1");
        return project(List.of(second, check("CHECK-3", OperationCheckType.API), implementation(), unrelated,
                operation(), check("CHECK-2", OperationCheckType.SQL), first,
                check("CHECK-1", OperationCheckType.API)), List.of(
                relationship("OP-IMPL", "OP", "IMPLEMENTED_BY", "IMPL"),
                relationship("T2-USES", "TEST-2", "USES", "IMPL"), firstUse, firstUse,
                relationship("T2-C3", "TEST-2", "HAS_CHECK", "CHECK-3"), firstCheck, firstCheck,
                relationship("T1-C2", "TEST-1", "HAS_CHECK", "CHECK-2")));
    }

    private static Project project(List<Node> nodes, List<Relationship> relationships) {
        return new Project("contract", "schema", new Metadata("P", "Project", null, null, Map.of()),
                List.of(), new Subject("OP"), nodes, relationships,
                new EvidenceManifest("evidence", "source", Map.of(), "normalization", "canonicalization",
                        "fingerprint", List.of(), List.of(), List.of()), List.of(), Map.of());
    }

    private static Node operation() {
        return node("OP", "BUSINESS_OPERATION", "POST /api/orders");
    }

    private static Node implementation() {
        return node("IMPL", "TECHNICAL_IMPLEMENTATION", "OrderController.create");
    }

    private static Node test(String id, String name, String evidenceText) {
        List<Map<String, Object>> references = evidenceText == null
                ? List.of()
                : List.of(Map.of("text", evidenceText));
        return new Node(id, "TEST_IMPLEMENTATION", name, null, "CONFIRMED",
                List.of(), references, Map.of(), Map.of());
    }

    private static Node check(String id, OperationCheckType type) {
        return new Node(id, "CHECK", id, null, "CONFIRMED", List.of(), List.of(), Map.of(),
                Map.of("check", Map.of("checkType", type.name(), "assertion", "assertThat(value)")));
    }

    private static Node node(String id, String type, String name) {
        return new Node(id, type, name, null, "CONFIRMED", List.of(), List.of(), Map.of(), Map.of());
    }

    private static Relationship relationship(String id, String from, String type, String to) {
        return new Relationship(id, from, type, to, Map.of(), List.of());
    }
}
