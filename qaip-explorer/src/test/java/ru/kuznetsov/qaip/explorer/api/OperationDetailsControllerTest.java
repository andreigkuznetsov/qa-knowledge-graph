package ru.kuznetsov.qaip.explorer.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.kuznetsov.qaip.explorer.application.OperationDetailsErrorCode;
import ru.kuznetsov.qaip.explorer.application.OperationDetailsException;
import ru.kuznetsov.qaip.explorer.application.OperationDetailsService;
import ru.kuznetsov.qaip.explorer.view.ImplementationPathView;
import ru.kuznetsov.qaip.explorer.view.OperationDetailsView;
import ru.kuznetsov.qaip.explorer.view.OperationVerificationStatus;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OperationDetailsControllerTest {
    private OperationDetailsService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(OperationDetailsService.class);
        mvc = MockMvcBuilders.standaloneSetup(new OperationDetailsController(service))
                .setControllerAdvice(new ExplorerApiExceptionHandler())
                .build();
    }

    @Test
    void delegates_and_returns_only_approved_json_shape() throws Exception {
        when(service.getDetails("P-1", "OP-1")).thenReturn(new OperationDetailsView(
                "OP-1", "POST", "/orders", "Create order", OperationVerificationStatus.VERIFIED, 2, 3,
                new ImplementationPathView("OrdersController", "OrderService", "OrderRepository")));

        mvc.perform(get("/api/v1/repositories/P-1/operations/OP-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationId").value("OP-1"))
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.path").value("/orders"))
                .andExpect(jsonPath("$.displayName").value("Create order"))
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.testCount").value(2))
                .andExpect(jsonPath("$.checkCount").value(3))
                .andExpect(jsonPath("$.implementationPath.controllerName").value("OrdersController"))
                .andExpect(jsonPath("$.implementationPath.serviceName").value("OrderService"))
                .andExpect(jsonPath("$.implementationPath.repositoryName").value("OrderRepository"))
                .andExpect(jsonPath("$.nodes").doesNotExist())
                .andExpect(jsonPath("$.relationships").doesNotExist())
                .andExpect(jsonPath("$.canonicalProjectJson").doesNotExist());

        verify(service).getDetails("P-1", "OP-1");
    }

    @Test
    void returns_typed_error_responses() throws Exception {
        assertError("repo", OperationDetailsErrorCode.REPOSITORY_NOT_FOUND, 404);
        assertError("operation", OperationDetailsErrorCode.OPERATION_NOT_FOUND, 404);
        assertError("incomplete", OperationDetailsErrorCode.INCOMPLETE_IMPLEMENTATION_PATH, 422);
        assertError("ambiguous", OperationDetailsErrorCode.AMBIGUOUS_IMPLEMENTATION_PATH, 422);
        assertError("analysis", OperationDetailsErrorCode.ANALYSIS_UNAVAILABLE, 409);
        assertError("runtime", OperationDetailsErrorCode.RUNTIME_FAILURE, 500);
    }

    private void assertError(String operationId, OperationDetailsErrorCode code, int statusCode) throws Exception {
        when(service.getDetails("P-1", operationId)).thenThrow(new OperationDetailsException(code, "Unavailable"));
        mvc.perform(get("/api/v1/repositories/P-1/operations/{operationId}", operationId))
                .andExpect(status().is(statusCode))
                .andExpect(jsonPath("$.code").value(code.name()))
                .andExpect(jsonPath("$.message").value("Unavailable"));
    }
}
