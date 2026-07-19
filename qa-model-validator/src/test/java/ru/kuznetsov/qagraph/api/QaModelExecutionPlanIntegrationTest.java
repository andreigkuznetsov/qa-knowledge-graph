package ru.kuznetsov.qagraph.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import ru.kuznetsov.qagraph.repository.InMemoryQaModelRepository;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class QaModelExecutionPlanIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired InMemoryQaModelRepository repository;

    @Test
    void shouldReturnAllThreeIndependentTasksInOneDeterministicWave()
            throws Exception {
        String modelId = registerModel(mixedModel());

        mockMvc.perform(get(
                        "/api/v1/models/{modelId}/execution-plan",
                        modelId
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelId").value(modelId))
                .andExpect(jsonPath("$.planned").value(true))
                .andExpect(jsonPath("$.schemaVersion").value("0.1"))
                .andExpect(jsonPath("$.summary.totalTasks").value(3))
                .andExpect(jsonPath("$.summary.totalWaves").value(1))
                .andExpect(jsonPath("$.summary.parallelizableTasks").value(3))
                .andExpect(jsonPath("$.summary.sequentialTasks").value(0))
                .andExpect(jsonPath("$.summary.maximumParallelism").value(3))
                .andExpect(jsonPath("$.waves", hasSize(1)))
                .andExpect(jsonPath("$.waves[0].number").value(1))
                .andExpect(jsonPath("$.waves[0].taskIds", hasSize(3)))
                .andExpect(jsonPath("$.waves[0].taskIds[0]")
                        .value("TASK-CREATE-SCENARIO-BR-002"))
                .andExpect(jsonPath("$.waves[0].taskIds[1]")
                        .value("TASK-CREATE-TEST-IMPLEMENTATION-SC-002"))
                .andExpect(jsonPath("$.waves[0].taskIds[2]")
                        .value("TASK-CREATE-CHECK-TC-001"))
                .andExpect(jsonPath("$.sourceRoadmapSummary.totalTasks")
                        .value(3))
                .andExpect(jsonPath("$.sourceRoadmapSummary.plannedTasks")
                        .value(3))
                .andExpect(jsonPath(
                        "$.sourceRoadmapSummary.tasksWithDependencies"
                ).value(0))
                .andExpect(jsonPath("$.validation.valid").value(true))
                .andExpect(jsonPath("$.tasks").doesNotExist())
                .andExpect(jsonPath("$.findings").doesNotExist())
                .andExpect(jsonPath("$.coverage").doesNotExist());
    }

    @Test
    void repeatedRequestShouldReturnIdenticalJson() throws Exception {
        String modelId = registerModel(mixedModel());

        assertEquals(executionPlanJson(modelId), executionPlanJson(modelId));
    }

    @Test
    void fullyCoveredModelShouldReturnSuccessfulEmptyPlan() throws Exception {
        String modelId = registerModel(validModel());

        mockMvc.perform(get(
                        "/api/v1/models/{modelId}/execution-plan",
                        modelId
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planned").value(true))
                .andExpect(jsonPath("$.summary.totalTasks").value(0))
                .andExpect(jsonPath("$.summary.totalWaves").value(0))
                .andExpect(jsonPath("$.summary.parallelizableTasks").value(0))
                .andExpect(jsonPath("$.summary.sequentialTasks").value(0))
                .andExpect(jsonPath("$.summary.maximumParallelism").value(0))
                .andExpect(jsonPath("$.waves", hasSize(0)))
                .andExpect(jsonPath("$.sourceRoadmapSummary.totalTasks")
                        .value(0))
                .andExpect(jsonPath("$.validation.valid").value(true));
    }

    @Test
    void unknownAndInvalidModelsShouldReuseExistingContracts()
            throws Exception {
        mockMvc.perform(get(
                        "/api/v1/models/{modelId}/execution-plan",
                        "unknown"
                ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("MODEL_NOT_FOUND"));

        String invalidId = repository.save(objectMapper.readTree("{}"), 0)
                .modelId();
        mockMvc.perform(get(
                        "/api/v1/models/{modelId}/execution-plan",
                        invalidId
                ))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.summary.errors")
                        .value(greaterThan(0)));
    }

    @Test
    void existingAnalysisEndpointsShouldRemainUnchanged() throws Exception {
        String modelId = registerModel(validModel());

        mockMvc.perform(get("/api/v1/models/{modelId}/coverage", modelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metrics", hasSize(3)))
                .andExpect(jsonPath("$.waves").doesNotExist());
        mockMvc.perform(get("/api/v1/models/{modelId}/findings", modelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.findings", hasSize(0)))
                .andExpect(jsonPath("$.waves").doesNotExist());
        mockMvc.perform(get("/api/v1/models/{modelId}/roadmap", modelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks", hasSize(0)))
                .andExpect(jsonPath("$.waves").doesNotExist())
                .andExpect(jsonPath("$.executionPlan").doesNotExist());
        mockMvc.perform(get("/api/v1/models/{modelId}/assessment", modelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.health").value("PASS"))
                .andExpect(jsonPath("$.waves").doesNotExist())
                .andExpect(jsonPath("$.executionPlan").doesNotExist());
    }

    private String executionPlanJson(String modelId) throws Exception {
        return mockMvc.perform(get(
                        "/api/v1/models/{modelId}/execution-plan",
                        modelId
                ))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String mixedModel() throws Exception {
        ObjectNode model = (ObjectNode) objectMapper.readTree(validModel());
        ArrayNode nodes = (ArrayNode) model.path("nodes");
        addCopy(nodes, "BR-001", "BR-002");
        addCopy(nodes, "SC-001", "SC-002");
        addCopy(nodes, "TC-001", "TC-002");
        for (JsonNode relationship : model.path("relationships")) {
            ObjectNode mutable = (ObjectNode) relationship;
            if ("VALIDATES".equals(relationship.path("type").asText())
                    || "HAS_CHECK".equals(
                            relationship.path("type").asText()
                    )) {
                mutable.put("from", "TC-002");
            }
        }
        return model.toString();
    }

    private void addCopy(ArrayNode nodes, String sourceId, String targetId) {
        for (JsonNode node : nodes) {
            if (sourceId.equals(node.path("id").asText())) {
                ObjectNode copy = node.deepCopy();
                copy.put("id", targetId);
                copy.put("name", "Uncovered " + targetId);
                nodes.add(copy);
                return;
            }
        }
        throw new IllegalStateException("Missing fixture node: " + sourceId);
    }

    private String registerModel(String model) throws Exception {
        String response = mockMvc.perform(post("/api/v1/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(model))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("modelId").asText();
    }

    private String validModel() throws Exception {
        return new ClassPathResource("valid-qa-model.json")
                .getContentAsString(StandardCharsets.UTF_8);
    }
}
