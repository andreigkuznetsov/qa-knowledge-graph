package ru.kuznetsov.qaip.explorer.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.kuznetsov.qaip.explorer.application.OperationListErrorCode;
import ru.kuznetsov.qaip.explorer.application.OperationListException;
import ru.kuznetsov.qaip.explorer.application.OperationListService;
import ru.kuznetsov.qaip.explorer.view.OperationListItemView;
import ru.kuznetsov.qaip.explorer.view.OperationListView;
import ru.kuznetsov.qaip.explorer.view.OperationVerificationStatus;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OperationListControllerTest {
    private OperationListService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(OperationListService.class);
        mvc = MockMvcBuilders.standaloneSetup(new OperationListController(service))
                .setControllerAdvice(new ExplorerApiExceptionHandler())
                .build();
    }

    @Test
    void delegates_and_returns_only_approved_view_fields() throws Exception {
        when(service.getOperations("PROJECT-1")).thenReturn(new OperationListView("PROJECT-1", List.of(
                new OperationListItemView("OP-1", "GET", "/orders", "List orders",
                        OperationVerificationStatus.VERIFIED, 2, 3))));

        mvc.perform(get("/api/v1/repositories/PROJECT-1/operations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositoryId").value("PROJECT-1"))
                .andExpect(jsonPath("$.operations.length()").value(1))
                .andExpect(jsonPath("$.operations[0].operationId").value("OP-1"))
                .andExpect(jsonPath("$.operations[0].method").value("GET"))
                .andExpect(jsonPath("$.operations[0].path").value("/orders"))
                .andExpect(jsonPath("$.operations[0].displayName").value("List orders"))
                .andExpect(jsonPath("$.operations[0].verificationStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.operations[0].testCount").value(2))
                .andExpect(jsonPath("$.operations[0].checkCount").value(3))
                .andExpect(jsonPath("$.operations[0].nodes").doesNotExist())
                .andExpect(jsonPath("$.operations[0].relationships").doesNotExist())
                .andExpect(jsonPath("$.operations[0].canonicalProjectJson").doesNotExist());

        verify(service).getOperations("PROJECT-1");
    }

    @Test
    void returns_typed_repository_analysis_and_runtime_errors() throws Exception {
        assertError("missing", OperationListErrorCode.REPOSITORY_NOT_FOUND,
                "Repository was not found", 404);
        assertError("pending", OperationListErrorCode.ANALYSIS_UNAVAILABLE,
                "Repository analysis is unavailable", 409);
        assertError("failed", OperationListErrorCode.RUNTIME_FAILURE,
                "Repository operations are unavailable", 500);
    }

    private void assertError(
            String repositoryId, OperationListErrorCode code, String message, int statusCode) throws Exception {
        when(service.getOperations(repositoryId)).thenThrow(new OperationListException(code, message));
        mvc.perform(get("/api/v1/repositories/{repositoryId}/operations", repositoryId))
                .andExpect(status().is(statusCode))
                .andExpect(jsonPath("$.code").value(code.name()))
                .andExpect(jsonPath("$.message").value(message));
    }
}
