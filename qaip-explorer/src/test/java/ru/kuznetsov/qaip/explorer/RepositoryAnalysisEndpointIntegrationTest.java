package ru.kuznetsov.qaip.explorer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.kuznetsov.qaip.explorer.api.AnalyzeRepositoryRequest;
import ru.kuznetsov.qaip.runtime.QaipRuntime;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RepositoryAnalysisEndpointIntegrationTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    MockMvc mvc;

    @Autowired
    QaipRuntime runtime;

    @TempDir
    Path repository;

    @Test
    void analyzes_focused_repository_and_imports_it_into_shared_runtime() throws Exception {
        Path source = repository.resolve("src/main/java/example/OrdersController.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, """
                package example;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RestController;
                @RestController
                class OrdersController {
                    @GetMapping("/orders") String orders() { return "orders"; }
                }
                """);
        byte[] request = JSON.writeValueAsBytes(
                new AnalyzeRepositoryRequest(repository.toString(), "Orders project"));

        String response = mvc.perform(post("/api/v1/repositories/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.analysisStatus").value("COMPLETE"))
                .andExpect(jsonPath("$.discoveredOperationCount").value(1))
                .andReturn().getResponse().getContentAsString();

        String repositoryId = JSON.readTree(response).get("repositoryId").textValue();
        assertTrue(runtime.projectReader().findById(repositoryId).isPresent());

        mvc.perform(post("/api/v1/repositories/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROJECT_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("Project already imported."))
                .andExpect(jsonPath("$.repositoryId").value(repositoryId));

        mvc.perform(get("/api/v1/repositories/{repositoryId}/summary", repositoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositoryId").value(repositoryId))
                .andExpect(jsonPath("$.projectIdentity").value(repositoryId))
                .andExpect(jsonPath("$.analysisStatus").value("COMPLETE"))
                .andExpect(jsonPath("$.operationCount").value(1))
                .andExpect(jsonPath("$.businessRuleCount").value(0))
                .andExpect(jsonPath("$.implementationNodeCount").value(1))
                .andExpect(jsonPath("$.testCount").value(0))
                .andExpect(jsonPath("$.checkCount").value(0))
                .andExpect(jsonPath("$.warningCount").value(0));

        String operations = mvc.perform(get("/api/v1/repositories/{repositoryId}/operations", repositoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositoryId").value(repositoryId))
                .andExpect(jsonPath("$.operations.length()").value(1))
                .andExpect(jsonPath("$.operations[0].method").value("GET"))
                .andExpect(jsonPath("$.operations[0].path").value("/orders"))
                .andExpect(jsonPath("$.operations[0].verificationStatus").value("UNVERIFIED"))
                .andExpect(jsonPath("$.operations[0].testCount").value(0))
                .andExpect(jsonPath("$.operations[0].checkCount").value(0))
                .andReturn().getResponse().getContentAsString();
        String operationId = JSON.readTree(operations).get("operations").get(0).get("operationId").textValue();

        mvc.perform(get("/api/v1/repositories/{repositoryId}/operations/{operationId}",
                        repositoryId, operationId))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INCOMPLETE_IMPLEMENTATION_PATH"))
                .andExpect(jsonPath("$.message").value("Operation implementation path is incomplete"));
    }
}
