package ru.kuznetsov.qaip.explorer.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.kuznetsov.qagraph.extractor.repositoryanalysis.RepositoryAnalysisStatus;
import ru.kuznetsov.qaip.explorer.application.RepositorySummaryErrorCode;
import ru.kuznetsov.qaip.explorer.application.RepositorySummaryException;
import ru.kuznetsov.qaip.explorer.application.RepositorySummaryService;
import ru.kuznetsov.qaip.explorer.view.RepositorySummaryView;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RepositorySummaryControllerTest {
    private RepositorySummaryService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(RepositorySummaryService.class);
        mvc = MockMvcBuilders.standaloneSetup(new RepositorySummaryController(service))
                .setControllerAdvice(new ExplorerApiExceptionHandler())
                .build();
    }

    @Test
    void delegates_and_returns_only_repository_summary_view_fields() throws Exception {
        when(service.getSummary("PROJECT-1")).thenReturn(new RepositorySummaryView(
                "PROJECT-1", "PROJECT-1", RepositoryAnalysisStatus.COMPLETE,
                2, 3, 4, 5, 6, 0));

        mvc.perform(get("/api/v1/repositories/PROJECT-1/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositoryId").value("PROJECT-1"))
                .andExpect(jsonPath("$.projectIdentity").value("PROJECT-1"))
                .andExpect(jsonPath("$.analysisStatus").value("COMPLETE"))
                .andExpect(jsonPath("$.operationCount").value(2))
                .andExpect(jsonPath("$.businessRuleCount").value(3))
                .andExpect(jsonPath("$.implementationNodeCount").value(4))
                .andExpect(jsonPath("$.testCount").value(5))
                .andExpect(jsonPath("$.checkCount").value(6))
                .andExpect(jsonPath("$.warningCount").value(0))
                .andExpect(jsonPath("$.canonicalProjectJson").doesNotExist())
                .andExpect(jsonPath("$.nodes").doesNotExist())
                .andExpect(jsonPath("$.relationships").doesNotExist())
                .andExpect(jsonPath("$.projectId").doesNotExist());

        verify(service).getSummary("PROJECT-1");
    }

    @Test
    void maps_repository_not_found_to_typed_response() throws Exception {
        when(service.getSummary("missing")).thenThrow(new RepositorySummaryException(
                RepositorySummaryErrorCode.REPOSITORY_NOT_FOUND, "Repository was not found"));

        mvc.perform(get("/api/v1/repositories/missing/summary"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REPOSITORY_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Repository was not found"));
    }
}
