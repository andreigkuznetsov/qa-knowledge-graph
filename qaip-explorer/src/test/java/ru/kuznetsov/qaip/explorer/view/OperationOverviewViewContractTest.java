package ru.kuznetsov.qaip.explorer.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OperationOverviewViewContractTest {
    @Test
    void exposes_unified_identity_and_available_section_payloads() throws Exception {
        OperationOverviewView overview = availableOverview();

        assertThat(overview.identity()).isEqualTo(new OperationOverviewIdentityView(
                "REPOSITORY-1", "OP-1", "POST", "/api/orders", "POST /api/orders"));
        assertThat(overview.implementation().state()).isEqualTo(OperationOverviewImplementationState.AVAILABLE);
        assertThat(overview.eventPath().state()).isEqualTo(OperationOverviewEventPathState.AVAILABLE);
        assertThat(overview.verification().state()).isEqualTo(OperationOverviewVerificationState.AVAILABLE);

        String json = new ObjectMapper().writeValueAsString(overview);
        assertThat(json).contains("\"state\":\"AVAILABLE\"");
        assertThat(json).contains("\"controllerName\":\"OrdersController.create\"");
        assertThat(json).contains("\"pathKind\":\"EVENT_DRIVEN\"");
        assertThat(json).contains("\"verificationStatus\":\"VERIFIED\"");
    }

    @Test
    void every_unavailable_section_state_is_an_explicit_payload_free_shape() {
        assertThat(new OperationOverviewImplementationIncompleteView(
                OperationOverviewImplementationState.INCOMPLETE).state())
                .isEqualTo(OperationOverviewImplementationState.INCOMPLETE);
        assertThat(new OperationOverviewImplementationAmbiguousView(
                OperationOverviewImplementationState.AMBIGUOUS).state())
                .isEqualTo(OperationOverviewImplementationState.AMBIGUOUS);
        assertThat(new OperationOverviewEventPathNotApplicableView(
                OperationOverviewEventPathState.NOT_APPLICABLE).state())
                .isEqualTo(OperationOverviewEventPathState.NOT_APPLICABLE);
        assertThat(new OperationOverviewEventPathIncompleteView(
                OperationOverviewEventPathState.INCOMPLETE).state())
                .isEqualTo(OperationOverviewEventPathState.INCOMPLETE);
        assertThat(new OperationOverviewEventPathAmbiguousView(
                OperationOverviewEventPathState.AMBIGUOUS).state())
                .isEqualTo(OperationOverviewEventPathState.AMBIGUOUS);
        assertThat(new OperationOverviewVerificationAmbiguousView(
                OperationOverviewVerificationState.AMBIGUOUS).state())
                .isEqualTo(OperationOverviewVerificationState.AMBIGUOUS);
    }

    @Test
    void partial_knowledge_sections_are_independent_and_overview_remains_constructible() {
        OperationOverviewView overview = new OperationOverviewView(identity(),
                new OperationOverviewImplementationIncompleteView(
                        OperationOverviewImplementationState.INCOMPLETE),
                new OperationOverviewEventPathAvailableView(
                        OperationOverviewEventPathState.AVAILABLE, eventPath()),
                new OperationOverviewVerificationAvailableView(
                        OperationOverviewVerificationState.AVAILABLE, verification()));

        assertThat(overview.implementation().state()).isEqualTo(OperationOverviewImplementationState.INCOMPLETE);
        assertThat(((OperationOverviewEventPathAvailableView) overview.eventPath()).path().steps())
                .hasSize(1);
        assertThat(((OperationOverviewVerificationAvailableView) overview.verification())
                .verification().testCount()).isEqualTo(1);
    }

    @Test
    void invalid_state_payload_combinations_and_missing_sections_are_rejected() {
        assertThatThrownBy(() -> new OperationOverviewImplementationAvailableView(
                OperationOverviewImplementationState.INCOMPLETE,
                new ImplementationPathView("C", "S", "R")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OperationOverviewEventPathAvailableView(
                OperationOverviewEventPathState.AMBIGUOUS, eventPath()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OperationOverviewVerificationAvailableView(
                OperationOverviewVerificationState.AMBIGUOUS, verification()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OperationOverviewView(identity(), null,
                new OperationOverviewEventPathNotApplicableView(
                        OperationOverviewEventPathState.NOT_APPLICABLE),
                new OperationOverviewVerificationAmbiguousView(OperationOverviewVerificationState.AMBIGUOUS)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void contract_reuses_presentation_payloads_and_has_no_runtime_or_framework_types() {
        Set<Class<?>> types = Set.of(
                OperationOverviewView.class, OperationOverviewIdentityView.class,
                OperationOverviewImplementationSectionView.class,
                OperationOverviewImplementationAvailableView.class,
                OperationOverviewImplementationIncompleteView.class,
                OperationOverviewImplementationAmbiguousView.class,
                OperationOverviewEventPathSectionView.class,
                OperationOverviewEventPathAvailableView.class,
                OperationOverviewEventPathNotApplicableView.class,
                OperationOverviewEventPathIncompleteView.class,
                OperationOverviewEventPathAmbiguousView.class,
                OperationOverviewVerificationSectionView.class,
                OperationOverviewVerificationAvailableView.class,
                OperationOverviewVerificationAmbiguousView.class);

        Set<String> componentTypes = types.stream().filter(Class::isRecord)
                .flatMap(type -> Arrays.stream(type.getRecordComponents()))
                .map(RecordComponent::getType).map(Class::getName).collect(Collectors.toSet());
        assertThat(componentTypes).noneMatch(name -> name.contains("qaip.core")
                || name.contains("springframework") || name.contains("persistence"));
        assertThat(OperationOverviewImplementationAvailableView.class.getRecordComponents()[1].getType())
                .isEqualTo(ImplementationPathView.class);
        assertThat(OperationOverviewEventPathAvailableView.class.getRecordComponents()[1].getType())
                .isEqualTo(EventPathView.class);
        assertThat(OperationOverviewVerificationAvailableView.class.getRecordComponents()[1].getType())
                .isEqualTo(OperationTestsView.class);
    }

    private static OperationOverviewView availableOverview() {
        return new OperationOverviewView(identity(),
                new OperationOverviewImplementationAvailableView(
                        OperationOverviewImplementationState.AVAILABLE,
                        new ImplementationPathView(
                                "OrdersController.create", "OrderService.create", "OrderRepository")),
                new OperationOverviewEventPathAvailableView(
                        OperationOverviewEventPathState.AVAILABLE, eventPath()),
                new OperationOverviewVerificationAvailableView(
                        OperationOverviewVerificationState.AVAILABLE, verification()));
    }

    private static OperationOverviewIdentityView identity() {
        return new OperationOverviewIdentityView(
                "REPOSITORY-1", "OP-1", "POST", "/api/orders", "POST /api/orders");
    }

    private static EventPathView eventPath() {
        return new EventPathView("REPOSITORY-1", "OP-1", "EVENT_DRIVEN", List.of(
                new EventPathStepView("REST_CONTROLLER", "OrdersController.create", "API", null)));
    }

    private static OperationTestsView verification() {
        OperationTestView test = new OperationTestView(
                "OrderApiIT.createsOrder", "example.OrderApiIT", "createsOrder", 1,
                List.of(new OperationCheckView("status is created", "API")));
        return new OperationTestsView("REPOSITORY-1", "OP-1",
                OperationVerificationStatus.VERIFIED, 1, 1, List.of(test));
    }
}
