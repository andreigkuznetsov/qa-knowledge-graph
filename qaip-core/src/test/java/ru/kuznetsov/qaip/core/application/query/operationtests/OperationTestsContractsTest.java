package ru.kuznetsov.qaip.core.application.query.operationtests;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathQuery;
import ru.kuznetsov.qaip.core.application.query.operationdetails.OperationDetailsQuery;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationTestsContractsTest {
    @Test
    void successful_result_contains_runtime_facing_test_and_check_data() {
        OperationTestsFound found = found(List.of(test("TEST-1", List.of(
                check("CHECK-2", OperationCheckType.SQL),
                check("CHECK-1", OperationCheckType.API)))));

        assertEquals("P-1", found.projectId());
        assertEquals("OP-1", found.operationId());
        assertEquals(OperationVerificationStatus.VERIFIED, found.verificationStatus());
        assertEquals("example.OrderApiIT", found.tests().getFirst().testClass());
        assertEquals("createsOrder", found.tests().getFirst().testMethod());
        assertEquals(List.of("CHECK-1", "CHECK-2"), found.tests().getFirst().checks().stream()
                .map(QualifiedOperationCheck::checkId).toList());
    }

    @Test
    void collections_are_defensively_copied_immutable_and_deterministically_ordered() {
        List<QualifiedOperationCheck> mutableChecks = new ArrayList<>(List.of(
                check("CHECK-2", OperationCheckType.SQL), check("CHECK-1", OperationCheckType.API)));
        QualifiedOperationTest second = test("TEST-2", mutableChecks);
        List<QualifiedOperationTest> mutableTests = new ArrayList<>(List.of(second, test("TEST-1", List.of())));

        OperationTestsFound found = found(mutableTests);
        mutableChecks.clear();
        mutableTests.clear();

        assertEquals(List.of("TEST-1", "TEST-2"), found.tests().stream()
                .map(QualifiedOperationTest::testId).toList());
        assertEquals(2, found.tests().get(1).checks().size());
        assertThrows(UnsupportedOperationException.class, () -> found.tests().clear());
        assertThrows(UnsupportedOperationException.class, () -> found.tests().get(1).checks().clear());
    }

    @Test
    void counts_and_verification_status_derive_from_returned_evidence() {
        OperationTestsFound found = found(List.of(
                test("TEST-2", List.of(check("CHECK-2", OperationCheckType.SQL))),
                test("TEST-1", List.of(check("CHECK-1", OperationCheckType.API)))));

        assertEquals(found.tests().size(), found.testCount());
        assertEquals(2, found.checkCount());
        assertEquals(found.tests().stream().mapToInt(QualifiedOperationTest::qualifiedCheckCount).sum(),
                found.checkCount());
        assertThrows(IllegalArgumentException.class, () -> new QualifiedOperationTest(
                "TEST", "Test", "example.Test", "test", 2, List.of(check("CHECK", OperationCheckType.API))));
        assertThrows(IllegalArgumentException.class, () -> new OperationTestsFound(
                "P", "OP", OperationVerificationStatus.PARTIALLY_VERIFIED,
                List.of(test("TEST", List.of(check("CHECK", OperationCheckType.API))))));
    }

    @Test
    void unavailable_outcomes_are_typed_and_validate_query_identity() {
        assertEquals(new OperationTestsProjectNotFound("P"), new OperationTestsProjectNotFound("P"));
        assertEquals(new OperationTestsOperationNotFound("P", "OP"),
                new OperationTestsOperationNotFound("P", "OP"));
        assertEquals(new OperationTestsNoneQualified("P", "OP"),
                new OperationTestsNoneQualified("P", "OP"));
        assertEquals(new OperationTestsAmbiguous("P", "OP"), new OperationTestsAmbiguous("P", "OP"));
        assertThrows(IllegalArgumentException.class, () -> new OperationTestsProjectNotFound(" "));
        assertThrows(IllegalArgumentException.class, () -> new OperationTestsNoneQualified("P", "\t"));
        assertThrows(IllegalArgumentException.class, () -> new OperationTestsFound(
                "P", "OP", OperationVerificationStatus.UNVERIFIED, List.of()));
    }

    @Test
    void result_hierarchy_has_exact_expected_outcomes() {
        assertTrue(OperationTestsQueryResult.class.isSealed());
        assertEquals(Set.of(OperationTestsFound.class, OperationTestsProjectNotFound.class,
                        OperationTestsOperationNotFound.class, OperationTestsNoneQualified.class,
                        OperationTestsAmbiguous.class),
                Arrays.stream(OperationTestsQueryResult.class.getPermittedSubclasses())
                        .collect(Collectors.toSet()));
    }

    @Test
    void query_boundary_uses_only_project_and_business_operation_identity() throws Exception {
        Method execute = OperationTestsQuery.class.getMethod("execute", String.class, String.class);

        assertEquals(OperationTestsQueryResult.class, execute.getReturnType());
        assertEquals(List.of(String.class, String.class), List.of(execute.getParameterTypes()));
        assertEquals(1, OperationTestsQuery.class.getMethods().length);
    }

    @Test
    void contracts_are_isolated_and_existing_runtime_queries_remain_available() throws Exception {
        List<Class<?>> contracts = List.of(OperationTestsQuery.class, OperationTestsQueryResult.class,
                OperationTestsFound.class, OperationTestsProjectNotFound.class,
                OperationTestsOperationNotFound.class, OperationTestsNoneQualified.class,
                OperationTestsAmbiguous.class, QualifiedOperationTest.class,
                QualifiedOperationCheck.class, OperationVerificationStatus.class, OperationCheckType.class);

        contracts.stream().flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
                .flatMap(method -> java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(method.getReturnType()), Arrays.stream(method.getParameterTypes())))
                .map(Class::getName)
                .forEach(name -> {
                    assertFalse(name.startsWith("org.springframework"));
                    assertFalse(name.startsWith("com.fasterxml.jackson"));
                    assertFalse(name.contains("explorer"));
                    assertFalse(name.contains("persistence"));
                    assertFalse(name.contains("extractor"));
                });
        assertEquals(2, OperationDetailsQuery.class.getMethod("execute", String.class, String.class)
                .getParameterCount());
        assertEquals(2, EventPathQuery.class.getMethod("execute", String.class, String.class)
                .getParameterCount());
    }

    private static OperationTestsFound found(List<QualifiedOperationTest> tests) {
        int checks = tests.stream().mapToInt(QualifiedOperationTest::qualifiedCheckCount).sum();
        return new OperationTestsFound("P-1", "OP-1",
                OperationVerificationStatus.fromCounts(tests.size(), checks), tests);
    }

    private static QualifiedOperationTest test(String id, List<QualifiedOperationCheck> checks) {
        return new QualifiedOperationTest(id, "OrderApiIT.createsOrder", "example.OrderApiIT",
                "createsOrder", checks.size(), checks);
    }

    private static QualifiedOperationCheck check(String id, OperationCheckType type) {
        return new QualifiedOperationCheck(id, "assertThat(response)", type);
    }
}
