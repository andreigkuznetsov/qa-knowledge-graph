package ru.kuznetsov.qaip.explorer.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.kuznetsov.qaip.explorer.application.EventPathProjectionAmbiguous;
import ru.kuznetsov.qaip.explorer.application.EventPathProjectionFound;
import ru.kuznetsov.qaip.explorer.application.EventPathProjectionIncomplete;
import ru.kuznetsov.qaip.explorer.application.EventPathProjectionNotEventDriven;
import ru.kuznetsov.qaip.explorer.application.EventPathProjectionOperationNotFound;
import ru.kuznetsov.qaip.explorer.application.EventPathProjectionProjectNotFound;
import ru.kuznetsov.qaip.explorer.application.GetEventPathService;
import ru.kuznetsov.qaip.explorer.view.EventPathStepView;
import ru.kuznetsov.qaip.explorer.view.EventPathView;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventPathControllerTest {
    private GetEventPathService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(GetEventPathService.class);
        mvc = MockMvcBuilders.standaloneSetup(new EventPathController(service)).build();
    }

    @Test
    void returns_ordered_presentation_only_event_path_and_invokes_service_once() throws Exception {
        when(service.getEventPath("P-1", "OP-1")).thenReturn(new EventPathProjectionFound(view()));

        mvc.perform(get("/api/v1/repositories/P-1/operations/OP-1/event-path"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value("P-1"))
                .andExpect(jsonPath("$.operationId").value("OP-1"))
                .andExpect(jsonPath("$.pathKind").value("EVENT_DRIVEN"))
                .andExpect(jsonPath("$.steps.length()").value(6))
                .andExpect(jsonPath("$.steps[0].implementationRole").value("REST_CONTROLLER"))
                .andExpect(jsonPath("$.steps[1].implementationRole").value("MESSAGE_PRODUCER"))
                .andExpect(jsonPath("$.steps[2].implementationRole").value("MESSAGE_DESTINATION"))
                .andExpect(jsonPath("$.steps[3].implementationRole").value("MESSAGE_CONSUMER"))
                .andExpect(jsonPath("$.steps[4].implementationRole").value("APPLICATION_SERVICE"))
                .andExpect(jsonPath("$.steps[5].implementationRole").value("REPOSITORY"))
                .andExpect(jsonPath("$.steps[1].technology").value("Kafka"))
                .andExpect(jsonPath("$.steps[0].technology").isEmpty())
                .andExpect(jsonPath("$.steps[0].nodeId").doesNotExist())
                .andExpect(jsonPath("$.relationships").doesNotExist())
                .andExpect(jsonPath("$.sourceReferences").doesNotExist());

        verify(service).getEventPath("P-1", "OP-1");
        verifyNoMoreInteractions(service);
    }

    @Test
    void maps_every_unavailable_result_to_its_approved_status_and_code() throws Exception {
        assertError("project", new EventPathProjectionProjectNotFound("P-1"), 404, "PROJECT_NOT_FOUND");
        assertError("operation", new EventPathProjectionOperationNotFound("P-1", "operation"),
                404, "OPERATION_NOT_FOUND");
        assertError("ambiguous", new EventPathProjectionAmbiguous("P-1", "ambiguous"),
                409, "AMBIGUOUS_EVENT_PATH");
        assertError("incomplete", new EventPathProjectionIncomplete("P-1", "incomplete"),
                422, "INCOMPLETE_EVENT_PATH");
        assertError("sync", new EventPathProjectionNotEventDriven("P-1", "sync"),
                422, "NOT_EVENT_DRIVEN");
    }

    private void assertError(
            String operationId,
            ru.kuznetsov.qaip.explorer.application.EventPathProjectionResult result,
            int statusCode,
            String code
    ) throws Exception {
        when(service.getEventPath("P-1", operationId)).thenReturn(result);
        mvc.perform(get("/api/v1/repositories/P-1/operations/{operationId}/event-path", operationId))
                .andExpect(status().is(statusCode))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    private static EventPathView view() {
        return new EventPathView("P-1", "OP-1", "EVENT_DRIVEN", List.of(
                step("REST_CONTROLLER", "OrdersController.create", "API", null),
                step("MESSAGE_PRODUCER", "OrdersController.create", "MESSAGE", "Kafka"),
                step("MESSAGE_DESTINATION", "orders.created", "MESSAGE", "Kafka"),
                step("MESSAGE_CONSUMER", "OrderCreatedListener.listen", "MESSAGE", "Kafka"),
                step("APPLICATION_SERVICE", "OrderProcessingService.process", "OTHER", null),
                step("REPOSITORY", "ProcessedOrderRepository", "DATABASE", null)));
    }

    private static EventPathStepView step(String role, String name, String type, String technology) {
        return new EventPathStepView(role, name, type, technology);
    }
}
