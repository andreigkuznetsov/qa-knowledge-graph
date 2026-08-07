package ru.kuznetsov.qaip.explorer.adapter.runtime;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.eventpath.*;
import ru.kuznetsov.qaip.core.application.query.operationoverview.*;
import ru.kuznetsov.qaip.core.application.query.operationtests.*;
import ru.kuznetsov.qaip.explorer.application.OperationOverviewProjectionFound;
import ru.kuznetsov.qaip.explorer.application.OperationOverviewProjectionOperationNotFound;
import ru.kuznetsov.qaip.explorer.application.OperationOverviewProjectionProjectNotFound;
import ru.kuznetsov.qaip.explorer.view.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OperationOverviewViewMapperTest {
    private final EventPathViewMapper eventPathMapper = new EventPathViewMapper();
    private final OperationTestsViewMapper operationTestsMapper = new OperationTestsViewMapper();
    private final OperationOverviewViewMapper mapper =
            new OperationOverviewViewMapper(eventPathMapper, operationTestsMapper);

    @Test
    void projects_found_overview_with_exact_runtime_values_and_existing_nested_projections() {
        EventPathResult path = eventPath();
        List<QualifiedOperationTest> tests = qualifiedTests();
        var runtime = found(
                new OperationOverviewImplementationAvailable(
                        new OperationOverviewImplementation("OrdersController", "OrdersService", "OrdersRepository")),
                new OperationOverviewEventPathAvailable(path),
                new OperationOverviewVerificationAvailable(
                        ru.kuznetsov.qaip.core.application.query.operationtests.OperationVerificationStatus.VERIFIED,
                        1, 1, tests));

        var result = (OperationOverviewProjectionFound) mapper.map(runtime);
        var overview = result.overview();

        assertThat(overview.identity()).isEqualTo(
                new OperationOverviewIdentityView("P-1", "OP-1", "POST", "/api/orders", "POST /api/orders"));
        assertThat(overview.implementation()).isEqualTo(new OperationOverviewImplementationAvailableView(
                ru.kuznetsov.qaip.explorer.view.OperationOverviewImplementationState.AVAILABLE,
                new ImplementationPathView("OrdersController", "OrdersService", "OrdersRepository")));
        assertThat(((OperationOverviewEventPathAvailableView) overview.eventPath()).path())
                .isEqualTo(eventPathMapper.mapPath(path));
        assertThat(((OperationOverviewVerificationAvailableView) overview.verification()).verification())
                .isEqualTo(operationTestsMapper.map(
                        "P-1", "OP-1",
                        ru.kuznetsov.qaip.core.application.query.operationtests.OperationVerificationStatus.VERIFIED,
                        1, 1, tests));
    }

    @Test
    void preserves_partial_knowledge_independently_and_maps_every_unavailable_state() {
        var partial = (OperationOverviewProjectionFound) mapper.map(found(
                new OperationOverviewImplementationIncomplete(),
                new OperationOverviewEventPathAmbiguous(),
                new OperationOverviewVerificationAvailable(
                        ru.kuznetsov.qaip.core.application.query.operationtests.OperationVerificationStatus.UNVERIFIED,
                        0, 0, List.of())));

        assertThat(partial.overview().implementation())
                .isInstanceOf(OperationOverviewImplementationIncompleteView.class);
        assertThat(partial.overview().eventPath())
                .isInstanceOf(OperationOverviewEventPathAmbiguousView.class);
        var verification = (OperationOverviewVerificationAvailableView) partial.overview().verification();
        assertThat(verification.verification().verificationStatus())
                .isEqualTo(ru.kuznetsov.qaip.explorer.view.OperationVerificationStatus.UNVERIFIED);
        assertThat(verification.verification().testCount()).isZero();
        assertThat(verification.verification().checkCount()).isZero();
        assertThat(verification.verification().tests()).isEmpty();

        var otherStates = (OperationOverviewProjectionFound) mapper.map(found(
                new OperationOverviewImplementationAmbiguous(),
                new OperationOverviewEventPathNotApplicable(),
                new OperationOverviewVerificationAmbiguous()));

        assertThat(otherStates.overview().implementation())
                .isInstanceOf(OperationOverviewImplementationAmbiguousView.class);
        assertThat(otherStates.overview().eventPath())
                .isInstanceOf(OperationOverviewEventPathNotApplicableView.class);
        assertThat(otherStates.overview().verification())
                .isInstanceOf(OperationOverviewVerificationAmbiguousView.class);
    }

    @Test
    void maps_event_path_incomplete_without_affecting_available_sibling_sections() {
        var result = (OperationOverviewProjectionFound) mapper.map(found(
                new OperationOverviewImplementationAvailable(
                        new OperationOverviewImplementation("Controller", "Service", "Repository")),
                new OperationOverviewEventPathIncomplete(),
                new OperationOverviewVerificationAvailable(
                        ru.kuznetsov.qaip.core.application.query.operationtests.OperationVerificationStatus.UNVERIFIED,
                        0, 0, List.of())));

        assertThat(result.overview().implementation())
                .isInstanceOf(OperationOverviewImplementationAvailableView.class);
        assertThat(result.overview().eventPath())
                .isInstanceOf(OperationOverviewEventPathIncompleteView.class);
        assertThat(result.overview().verification())
                .isInstanceOf(OperationOverviewVerificationAvailableView.class);
    }

    @Test
    void maps_top_level_not_found_results_without_invoking_section_projection() {
        assertThat(mapper.map(new OperationOverviewProjectNotFound("P-1")))
                .isEqualTo(new OperationOverviewProjectionProjectNotFound("P-1"));
        assertThat(mapper.map(new OperationOverviewOperationNotFound("P-1", "OP-404")))
                .isEqualTo(new OperationOverviewProjectionOperationNotFound("P-1", "OP-404"));
    }

    private static OperationOverviewFound found(
            OperationOverviewImplementationSection implementation,
            OperationOverviewEventPathSection eventPath,
            OperationOverviewVerificationSection verification
    ) {
        return new OperationOverviewFound(
                new OperationOverviewIdentity("P-1", "OP-1", "POST", "/api/orders", "POST /api/orders"),
                implementation, eventPath, verification);
    }

    private static List<QualifiedOperationTest> qualifiedTests() {
        return List.of(new QualifiedOperationTest(
                "T-1", "creates an order", "OrdersApiTest", "createsOrder", 1,
                List.of(new QualifiedOperationCheck("C-1", "status is created", OperationCheckType.API))));
    }

    private static EventPathResult eventPath() {
        return new EventPathResult("P-1", "OP-1", EventPathKind.EVENT_DRIVEN, List.of(
                step("C", EventPathImplementationRole.REST_CONTROLLER, "OrdersController.create",
                        EventPathImplementationType.API, null),
                step("P", EventPathImplementationRole.MESSAGE_PRODUCER, "OrdersController.create",
                        EventPathImplementationType.MESSAGE, "Kafka"),
                step("D", EventPathImplementationRole.MESSAGE_DESTINATION, "orders.created",
                        EventPathImplementationType.MESSAGE, "Kafka"),
                step("M", EventPathImplementationRole.MESSAGE_CONSUMER, "OrderCreatedListener.listen",
                        EventPathImplementationType.MESSAGE, "Kafka"),
                step("S", EventPathImplementationRole.APPLICATION_SERVICE, "OrdersService.process",
                        EventPathImplementationType.OTHER, null),
                step("R", EventPathImplementationRole.REPOSITORY, "OrdersRepository",
                        EventPathImplementationType.DATABASE, null)));
    }

    private static EventPathStep step(
            String id,
            EventPathImplementationRole role,
            String name,
            EventPathImplementationType type,
            String technology
    ) {
        return new EventPathStep(id, role, name, type, technology);
    }
}
