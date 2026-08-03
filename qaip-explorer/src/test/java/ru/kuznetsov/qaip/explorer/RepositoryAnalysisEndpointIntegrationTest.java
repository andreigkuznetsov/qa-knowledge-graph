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
    }
}
