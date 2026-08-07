package ru.kuznetsov.qaip.core.application.query.operationoverview;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathImplementationRole;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathImplementationType;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathKind;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathResult;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathStep;
import ru.kuznetsov.qaip.core.application.query.operationtests.OperationCheckType;
import ru.kuznetsov.qaip.core.application.query.operationtests.OperationVerificationStatus;
import ru.kuznetsov.qaip.core.application.query.operationtests.QualifiedOperationCheck;
import ru.kuznetsov.qaip.core.application.query.operationtests.QualifiedOperationTest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationOverviewContractsTest {
    @Test
    void overview_construction_preserves_identity_and_available_specialized_values() {
        EventPathResult path = eventPath("P-1", "OP-1");
        QualifiedOperationTest test = test("TEST-1", List.of(check("CHECK-1")));
        OperationOverviewFound found = found(
                new OperationOverviewImplementationAvailable(new OperationOverviewImplementation(
                        "OrdersController.create", "OrderService.create", "OrderRepository")),
                new OperationOverviewEventPathAvailable(path),
                new OperationOverviewVerificationAvailable(
                        OperationVerificationStatus.VERIFIED, 1, 1, List.of(test)));

        assertEquals("POST", found.identity().method());
        assertEquals(OperationOverviewImplementationState.AVAILABLE, found.implementation().state());
        assertEquals(path, ((OperationOverviewEventPathAvailable) found.eventPath()).path());
        assertEquals(List.of(test),
                ((OperationOverviewVerificationAvailable) found.verification()).tests());
    }

    @Test
    void top_level_not_found_results_validate_query_identity() {
        assertEquals(new OperationOverviewProjectNotFound("P"), new OperationOverviewProjectNotFound("P"));
        assertEquals(new OperationOverviewOperationNotFound("P", "OP"),
                new OperationOverviewOperationNotFound("P", "OP"));
        assertThrows(NullPointerException.class, () -> new OperationOverviewProjectNotFound(null));
        assertThrows(IllegalArgumentException.class, () -> new OperationOverviewProjectNotFound(" "));
        assertThrows(IllegalArgumentException.class,
                () -> new OperationOverviewOperationNotFound("P", "\t"));
    }

    @Test
    void every_section_state_is_typed_and_reports_its_explicit_state() {
        assertEquals(OperationOverviewImplementationState.AVAILABLE,
                implementationAvailable().state());
        assertEquals(OperationOverviewImplementationState.INCOMPLETE,
                new OperationOverviewImplementationIncomplete().state());
        assertEquals(OperationOverviewImplementationState.AMBIGUOUS,
                new OperationOverviewImplementationAmbiguous().state());

        assertEquals(OperationOverviewEventPathState.AVAILABLE,
                new OperationOverviewEventPathAvailable(eventPath("P-1", "OP-1")).state());
        assertEquals(OperationOverviewEventPathState.NOT_APPLICABLE,
                new OperationOverviewEventPathNotApplicable().state());
        assertEquals(OperationOverviewEventPathState.INCOMPLETE,
                new OperationOverviewEventPathIncomplete().state());
        assertEquals(OperationOverviewEventPathState.AMBIGUOUS,
                new OperationOverviewEventPathAmbiguous().state());

        assertEquals(OperationOverviewVerificationState.AVAILABLE, unverified().state());
        assertEquals(OperationOverviewVerificationState.AMBIGUOUS,
                new OperationOverviewVerificationAmbiguous().state());
    }

    @Test
    void missing_knowledge_in_each_section_does_not_prevent_an_overview() {
        OperationOverviewFound found = found(
                new OperationOverviewImplementationIncomplete(),
                new OperationOverviewEventPathNotApplicable(),
                unverified());

        assertEquals("OP-1", found.identity().operationId());
        assertEquals(OperationOverviewImplementationState.INCOMPLETE, found.implementation().state());
        assertEquals(OperationOverviewEventPathState.NOT_APPLICABLE, found.eventPath().state());
        OperationOverviewVerificationAvailable verification =
                assertInstanceOf(OperationOverviewVerificationAvailable.class, found.verification());
        assertEquals(OperationVerificationStatus.UNVERIFIED, verification.verificationStatus());
        assertEquals(0, verification.testCount());
        assertEquals(0, verification.checkCount());
        assertEquals(List.of(), verification.tests());
    }

    @Test
    void verification_collections_are_defensively_copied_immutable_and_deterministically_ordered() {
        List<QualifiedOperationTest> mutable = new ArrayList<>(List.of(
                test("TEST-2", List.of()), test("TEST-1", List.of())));
        OperationOverviewVerificationAvailable available = new OperationOverviewVerificationAvailable(
                OperationVerificationStatus.PARTIALLY_VERIFIED, 2, 0, mutable);
        mutable.clear();

        assertEquals(List.of("TEST-1", "TEST-2"),
                available.tests().stream().map(QualifiedOperationTest::testId).toList());
        assertThrows(UnsupportedOperationException.class, () -> available.tests().clear());
    }

    @Test
    void invalid_payload_and_state_combinations_are_rejected() {
        QualifiedOperationTest checked = test("TEST", List.of(check("CHECK")));
        QualifiedOperationTest unchecked = test("TEST", List.of());

        assertThrows(NullPointerException.class,
                () -> new OperationOverviewImplementationAvailable(null));
        assertThrows(NullPointerException.class,
                () -> new OperationOverviewEventPathAvailable(null));
        assertThrows(NullPointerException.class,
                () -> new OperationOverviewVerificationAvailable(null, 0, 0, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new OperationOverviewVerificationAvailable(
                        OperationVerificationStatus.VERIFIED, 0, 0, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new OperationOverviewVerificationAvailable(
                        OperationVerificationStatus.UNVERIFIED, 1, 0, List.of(unchecked)));
        assertThrows(IllegalArgumentException.class,
                () -> new OperationOverviewVerificationAvailable(
                        OperationVerificationStatus.PARTIALLY_VERIFIED, 1, 1, List.of(checked)));
        assertThrows(IllegalArgumentException.class,
                () -> new OperationOverviewVerificationAvailable(
                        OperationVerificationStatus.VERIFIED, 2, 1, List.of(checked)));
        assertThrows(IllegalArgumentException.class, () -> found(
                implementationAvailable(),
                new OperationOverviewEventPathAvailable(eventPath("OTHER", "OP-1")),
                unverified()));
    }

    @Test
    void result_and_section_hierarchies_have_exact_outcomes() {
        assertPermits(OperationOverviewQueryResult.class, Set.of(
                OperationOverviewFound.class, OperationOverviewProjectNotFound.class,
                OperationOverviewOperationNotFound.class));
        assertPermits(OperationOverviewImplementationSection.class, Set.of(
                OperationOverviewImplementationAvailable.class,
                OperationOverviewImplementationIncomplete.class,
                OperationOverviewImplementationAmbiguous.class));
        assertPermits(OperationOverviewEventPathSection.class, Set.of(
                OperationOverviewEventPathAvailable.class,
                OperationOverviewEventPathNotApplicable.class,
                OperationOverviewEventPathIncomplete.class,
                OperationOverviewEventPathAmbiguous.class));
        assertPermits(OperationOverviewVerificationSection.class, Set.of(
                OperationOverviewVerificationAvailable.class,
                OperationOverviewVerificationAmbiguous.class));
    }

    @Test
    void query_boundary_and_contracts_are_isolated_from_framework_explorer_and_persistence() throws Exception {
        Method execute = OperationOverviewQuery.class.getMethod("execute", String.class, String.class);
        assertEquals(OperationOverviewQueryResult.class, execute.getReturnType());
        assertEquals(List.of(String.class, String.class), List.of(execute.getParameterTypes()));
        assertEquals(1, OperationOverviewQuery.class.getMethods().length);

        List<Class<?>> contracts = List.of(
                OperationOverviewQuery.class, OperationOverviewQueryResult.class,
                OperationOverviewFound.class, OperationOverviewProjectNotFound.class,
                OperationOverviewOperationNotFound.class, OperationOverviewIdentity.class,
                OperationOverviewImplementationSection.class, OperationOverviewImplementationAvailable.class,
                OperationOverviewImplementationIncomplete.class, OperationOverviewImplementationAmbiguous.class,
                OperationOverviewEventPathSection.class, OperationOverviewEventPathAvailable.class,
                OperationOverviewEventPathNotApplicable.class, OperationOverviewEventPathIncomplete.class,
                OperationOverviewEventPathAmbiguous.class, OperationOverviewVerificationSection.class,
                OperationOverviewVerificationAvailable.class, OperationOverviewVerificationAmbiguous.class);

        contracts.stream().flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
                .flatMap(method -> java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(method.getReturnType()),
                        Arrays.stream(method.getParameterTypes())))
                .map(Class::getName)
                .forEach(name -> {
                    assertFalse(name.startsWith("org.springframework"));
                    assertFalse(name.startsWith("com.fasterxml.jackson"));
                    assertFalse(name.contains("explorer"));
                    assertFalse(name.contains("persistence"));
                    assertFalse(name.contains("extractor"));
                });
    }

    private static OperationOverviewFound found(
            OperationOverviewImplementationSection implementation,
            OperationOverviewEventPathSection eventPath,
            OperationOverviewVerificationSection verification) {
        return new OperationOverviewFound(
                new OperationOverviewIdentity("P-1", "OP-1", "POST", "/orders", "POST /orders"),
                implementation, eventPath, verification);
    }

    private static OperationOverviewImplementationAvailable implementationAvailable() {
        return new OperationOverviewImplementationAvailable(new OperationOverviewImplementation(
                "Controller", "Service", "Repository"));
    }

    private static OperationOverviewVerificationAvailable unverified() {
        return new OperationOverviewVerificationAvailable(
                OperationVerificationStatus.UNVERIFIED, 0, 0, List.of());
    }

    private static QualifiedOperationTest test(String id, List<QualifiedOperationCheck> checks) {
        return new QualifiedOperationTest(id, "OrderApiIT.createsOrder", "example.OrderApiIT",
                "createsOrder", checks.size(), checks);
    }

    private static QualifiedOperationCheck check(String id) {
        return new QualifiedOperationCheck(id, "assertThat(response)", OperationCheckType.API);
    }

    private static EventPathResult eventPath(String projectId, String operationId) {
        return new EventPathResult(projectId, operationId, EventPathKind.EVENT_DRIVEN, List.of(
                step("CONTROLLER", EventPathImplementationRole.REST_CONTROLLER,
                        EventPathImplementationType.API),
                step("PRODUCER", EventPathImplementationRole.MESSAGE_PRODUCER,
                        EventPathImplementationType.MESSAGE),
                step("DESTINATION", EventPathImplementationRole.MESSAGE_DESTINATION,
                        EventPathImplementationType.MESSAGE),
                step("CONSUMER", EventPathImplementationRole.MESSAGE_CONSUMER,
                        EventPathImplementationType.MESSAGE),
                step("SERVICE", EventPathImplementationRole.APPLICATION_SERVICE,
                        EventPathImplementationType.OTHER),
                step("REPOSITORY", EventPathImplementationRole.REPOSITORY,
                        EventPathImplementationType.DATABASE)));
    }

    private static EventPathStep step(
            String id, EventPathImplementationRole role, EventPathImplementationType type) {
        return new EventPathStep(id, role, id, type, null);
    }

    private static void assertPermits(Class<?> type, Set<Class<?>> expected) {
        assertTrue(type.isSealed());
        assertEquals(expected, Arrays.stream(type.getPermittedSubclasses()).collect(Collectors.toSet()));
    }
}
