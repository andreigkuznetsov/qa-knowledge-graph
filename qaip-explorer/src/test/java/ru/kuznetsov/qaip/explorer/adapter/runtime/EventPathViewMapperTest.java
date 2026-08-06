package ru.kuznetsov.qaip.explorer.adapter.runtime;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathFound;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathImplementationRole;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathImplementationType;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathKind;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathResult;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathStep;
import ru.kuznetsov.qaip.explorer.view.EventPathStepView;
import ru.kuznetsov.qaip.explorer.view.EventPathView;
import ru.kuznetsov.qaip.explorer.view.OperationDetailsView;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventPathViewMapperTest {
    private final EventPathViewMapper mapper = new EventPathViewMapper();

    @Test
    void projects_found_path_without_node_ids_and_preserves_runtime_order() {
        EventPathView view = mapper.map(found());

        assertThat(view.projectId()).isEqualTo("P-1");
        assertThat(view.operationId()).isEqualTo("OP-1");
        assertThat(view.pathKind()).isEqualTo("EVENT_DRIVEN");
        assertThat(view.steps()).extracting(EventPathStepView::implementationRole).containsExactly(
                "REST_CONTROLLER",
                "MESSAGE_PRODUCER",
                "MESSAGE_DESTINATION",
                "MESSAGE_CONSUMER",
                "APPLICATION_SERVICE",
                "REPOSITORY");
        assertThat(view.steps()).extracting(EventPathStepView::displayName).containsExactly(
                "OrdersController.create", "OrdersController.create", "orders.created",
                "OrderCreatedListener.listen", "OrderProcessingService.process", "ProcessedOrderRepository");
        assertThat(view.steps()).extracting(EventPathStepView::implementationType).containsExactly(
                "API", "MESSAGE", "MESSAGE", "MESSAGE", "OTHER", "DATABASE");
        assertThat(view.steps()).extracting(EventPathStepView::technology).containsExactly(
                null, "Kafka", "Kafka", "Kafka", null, null);
    }

    @Test
    void view_steps_are_immutable_and_defensively_copied() {
        List<EventPathStepView> source = new ArrayList<>(mapper.map(found()).steps());
        EventPathView view = new EventPathView("P-1", "OP-1", "EVENT_DRIVEN", source);
        source.clear();

        assertThat(view.steps()).hasSize(6);
        assertThatThrownBy(() -> view.steps().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void view_models_expose_only_presentation_fields_and_have_no_runtime_components() {
        assertThat(componentNames(EventPathView.class)).containsExactly(
                "projectId", "operationId", "pathKind", "steps");
        assertThat(componentNames(EventPathStepView.class)).containsExactly(
                "implementationRole", "displayName", "implementationType", "technology");
        Arrays.stream(EventPathView.class.getRecordComponents())
                .map(RecordComponent::getType).map(Class::getName)
                .forEach(name -> assertThat(name).doesNotContain("qaip.core", "persistence"));
        Arrays.stream(EventPathStepView.class.getRecordComponents())
                .map(RecordComponent::getType).map(Class::getName)
                .forEach(name -> assertThat(name).doesNotContain("qaip.core", "persistence"));
        assertThat(componentNames(EventPathStepView.class)).doesNotContain(
                "nodeId", "relationships", "sourceReferences");
        assertThat(EventPathView.class.getAnnotations()).isEmpty();
        assertThat(EventPathStepView.class.getAnnotations()).isEmpty();
    }

    @Test
    void models_reject_invalid_required_values_and_blank_optional_technology() {
        assertThatNullPointerException().isThrownBy(() -> mapper.map(null));
        assertThatNullPointerException().isThrownBy(() ->
                new EventPathView(null, "OP", "EVENT_DRIVEN", List.of()));
        assertThatThrownBy(() -> new EventPathStepView("REPOSITORY", "Repository", "DATABASE", " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void existing_operation_details_view_remains_separate_and_unchanged() {
        assertThat(componentNames(OperationDetailsView.class)).containsExactly(
                "operationId", "method", "path", "displayName", "verificationStatus",
                "testCount", "checkCount", "implementationPath");
    }

    private static List<String> componentNames(Class<?> type) {
        return Arrays.stream(type.getRecordComponents()).map(RecordComponent::getName).toList();
    }

    private static EventPathFound found() {
        return new EventPathFound(new EventPathResult("P-1", "OP-1", EventPathKind.EVENT_DRIVEN, List.of(
                step("C", EventPathImplementationRole.REST_CONTROLLER,
                        "OrdersController.create", EventPathImplementationType.API, null),
                step("P", EventPathImplementationRole.MESSAGE_PRODUCER,
                        "OrdersController.create", EventPathImplementationType.MESSAGE, "Kafka"),
                step("D", EventPathImplementationRole.MESSAGE_DESTINATION,
                        "orders.created", EventPathImplementationType.MESSAGE, "Kafka"),
                step("M", EventPathImplementationRole.MESSAGE_CONSUMER,
                        "OrderCreatedListener.listen", EventPathImplementationType.MESSAGE, "Kafka"),
                step("S", EventPathImplementationRole.APPLICATION_SERVICE,
                        "OrderProcessingService.process", EventPathImplementationType.OTHER, null),
                step("R", EventPathImplementationRole.REPOSITORY,
                        "ProcessedOrderRepository", EventPathImplementationType.DATABASE, null))));
    }

    private static EventPathStep step(
            String id, EventPathImplementationRole role, String name,
            EventPathImplementationType type, String technology) {
        return new EventPathStep(id, role, name, type, technology);
    }
}
