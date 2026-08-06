package ru.kuznetsov.qaip.explorer.adapter.runtime;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathAmbiguous;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathFound;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathImplementationRole;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathImplementationType;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathIncomplete;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathKind;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathNotEventDriven;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathOperationNotFound;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathProjectNotFound;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathQuery;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathResult;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathStep;
import ru.kuznetsov.qaip.explorer.application.EventPathProjectionAmbiguous;
import ru.kuznetsov.qaip.explorer.application.EventPathProjectionFound;
import ru.kuznetsov.qaip.explorer.application.EventPathProjectionIncomplete;
import ru.kuznetsov.qaip.explorer.application.EventPathProjectionNotEventDriven;
import ru.kuznetsov.qaip.explorer.application.EventPathProjectionOperationNotFound;
import ru.kuznetsov.qaip.explorer.application.EventPathProjectionProjectNotFound;
import ru.kuznetsov.qaip.explorer.application.EventPathProjectionResult;
import ru.kuznetsov.qaip.explorer.application.GetEventPathService;
import ru.kuznetsov.qaip.explorer.view.EventPathStepView;
import ru.kuznetsov.qaip.explorer.view.EventPathView;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class RuntimeEventPathProjectionServiceTest {
    private final EventPathQuery query = mock(EventPathQuery.class);
    private final EventPathViewMapper mapper = mock(EventPathViewMapper.class);
    private final GetEventPathService service = new RuntimeEventPathProjectionService(query, mapper);

    @Test
    void invokes_runtime_once_and_projects_found_result_through_mapper() {
        EventPathFound found = found();
        EventPathView projected = projected();
        when(query.execute("P-1", "OP-1")).thenReturn(found);
        when(mapper.map(found)).thenReturn(projected);

        EventPathProjectionFound result = (EventPathProjectionFound)
                service.getEventPath("P-1", "OP-1");

        assertThat(result.path()).isSameAs(projected);
        assertThat(result.path().pathKind()).isEqualTo("EVENT_DRIVEN");
        assertThat(result.path().steps()).extracting(EventPathStepView::implementationRole).containsExactly(
                "REST_CONTROLLER", "MESSAGE_PRODUCER", "MESSAGE_DESTINATION",
                "MESSAGE_CONSUMER", "APPLICATION_SERVICE", "REPOSITORY");
        assertThatThrownBy(() -> result.path().steps().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        verify(query).execute("P-1", "OP-1");
        verify(mapper).map(found);
        verifyNoMoreInteractions(query, mapper);
    }

    @Test
    void maps_every_unavailable_runtime_result_to_one_explorer_owned_result_without_mapper_use() {
        assertMaps(new EventPathProjectNotFound("P"), new EventPathProjectionProjectNotFound("P"));
        assertMaps(new EventPathOperationNotFound("P", "OP"),
                new EventPathProjectionOperationNotFound("P", "OP"));
        assertMaps(new EventPathIncomplete("P", "OP"), new EventPathProjectionIncomplete("P", "OP"));
        assertMaps(new EventPathAmbiguous("P", "OP"), new EventPathProjectionAmbiguous("P", "OP"));
        assertMaps(new EventPathNotEventDriven("P", "OP"),
                new EventPathProjectionNotEventDriven("P", "OP"));

        verify(mapper, never()).map(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void service_dependencies_exclude_graph_and_synchronous_fallback_collaborators() {
        assertThat(RuntimeEventPathProjectionService.class.getDeclaredFields())
                .extracting(field -> field.getType().getName())
                .containsExactlyInAnyOrder(EventPathQuery.class.getName(), EventPathViewMapper.class.getName());
        assertThat(GetEventPathService.class.getDeclaredMethods()).hasSize(1);
        assertThat(EventPathProjectionResult.class.isSealed()).isTrue();
        assertThat(EventPathProjectionResult.class.getPermittedSubclasses()).containsExactlyInAnyOrder(
                EventPathProjectionFound.class,
                EventPathProjectionProjectNotFound.class,
                EventPathProjectionOperationNotFound.class,
                EventPathProjectionIncomplete.class,
                EventPathProjectionAmbiguous.class,
                EventPathProjectionNotEventDriven.class);
    }

    private void assertMaps(
            ru.kuznetsov.qaip.core.application.query.eventpath.EventPathQueryResult runtimeResult,
            EventPathProjectionResult expected) {
        when(query.execute("P", "OP")).thenReturn(runtimeResult);

        assertThat(service.getEventPath("P", "OP")).isEqualTo(expected);

        verify(query).execute("P", "OP");
        org.mockito.Mockito.clearInvocations(query);
    }

    private static EventPathFound found() {
        return new EventPathFound(new EventPathResult("P-1", "OP-1", EventPathKind.EVENT_DRIVEN, List.of(
                step("C", EventPathImplementationRole.REST_CONTROLLER, EventPathImplementationType.API),
                step("P", EventPathImplementationRole.MESSAGE_PRODUCER, EventPathImplementationType.MESSAGE),
                step("D", EventPathImplementationRole.MESSAGE_DESTINATION, EventPathImplementationType.MESSAGE),
                step("M", EventPathImplementationRole.MESSAGE_CONSUMER, EventPathImplementationType.MESSAGE),
                step("S", EventPathImplementationRole.APPLICATION_SERVICE, EventPathImplementationType.OTHER),
                step("R", EventPathImplementationRole.REPOSITORY, EventPathImplementationType.DATABASE))));
    }

    private static EventPathStep step(
            String id, EventPathImplementationRole role, EventPathImplementationType type) {
        return new EventPathStep(id, role, role.name(), type, null);
    }

    private static EventPathView projected() {
        return new EventPathView("P-1", "OP-1", "EVENT_DRIVEN", List.of(
                viewStep("REST_CONTROLLER"), viewStep("MESSAGE_PRODUCER"),
                viewStep("MESSAGE_DESTINATION"), viewStep("MESSAGE_CONSUMER"),
                viewStep("APPLICATION_SERVICE"), viewStep("REPOSITORY")));
    }

    private static EventPathStepView viewStep(String role) {
        return new EventPathStepView(role, role, "OTHER", null);
    }
}
