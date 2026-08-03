package ru.kuznetsov.qaip.explorer.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.kuznetsov.qagraph.extractor.repositoryanalysis.RepositoryAnalysisStatus;
import ru.kuznetsov.qaip.explorer.application.AnalyzeRepositoryCommand;
import ru.kuznetsov.qaip.explorer.application.AnalyzeRepositoryOutcome;
import ru.kuznetsov.qaip.explorer.application.AnalyzeRepositoryService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RepositoryAnalysisControllerTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    private AnalyzeRepositoryService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(AnalyzeRepositoryService.class);
        mvc = MockMvcBuilders.standaloneSetup(new RepositoryAnalysisController(service))
                .setControllerAdvice(new ExplorerApiExceptionHandler())
                .build();
    }

    @Test
    void delegates_to_application_service_and_returns_only_product_fields() throws Exception {
        when(service.analyze(any())).thenReturn(new AnalyzeRepositoryOutcome(
                "PROJECT-1", "PROJECT-1", RepositoryAnalysisStatus.COMPLETE, 2, List.of()));
        var request = new AnalyzeRepositoryRequest("repository", "Example project");

        mvc.perform(post("/api/v1/repositories/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.repositoryId").value("PROJECT-1"))
                .andExpect(jsonPath("$.projectIdentity").value("PROJECT-1"))
                .andExpect(jsonPath("$.analysisStatus").value("COMPLETE"))
                .andExpect(jsonPath("$.discoveredOperationCount").value(2))
                .andExpect(jsonPath("$.warnings").isArray())
                .andExpect(jsonPath("$.canonicalProjectJson").doesNotExist())
                .andExpect(jsonPath("$.nodes").doesNotExist())
                .andExpect(jsonPath("$.relationships").doesNotExist());

        verify(service).analyze(new AnalyzeRepositoryCommand("repository", "Example project"));
    }

    @Test
    void invalid_request_returns_typed_bad_request() throws Exception {
        mvc.perform(post("/api/v1/repositories/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"repositoryPath\":\"repository\",\"projectName\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REPOSITORY_INPUT"));
    }
}
