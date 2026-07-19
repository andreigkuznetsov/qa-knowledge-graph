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
class QaModelRoadmapIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired InMemoryQaModelRepository repository;

    @Test
    void shouldReturnDeterministicRoadmapForAllThreeGaps() throws Exception {
        String modelId = registerModel(mixedModel());

        mockMvc.perform(get("/api/v1/models/{modelId}/roadmap", modelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelId").value(modelId))
                .andExpect(jsonPath("$.planned").value(true))
                .andExpect(jsonPath("$.schemaVersion").value("0.1"))
                .andExpect(jsonPath("$.summary.totalTasks").value(3))
                .andExpect(jsonPath("$.summary.plannedTasks").value(3))
                .andExpect(jsonPath("$.summary.tasksWithDependencies").value(0))
                .andExpect(jsonPath("$.tasks", hasSize(3)))
                .andExpect(jsonPath("$.tasks[0].id")
                        .value("TASK-CREATE-SCENARIO-BR-002"))
                .andExpect(jsonPath("$.tasks[0].type").value("CREATE_SCENARIO"))
                .andExpect(jsonPath("$.tasks[0].status").value("PLANNED"))
                .andExpect(jsonPath("$.tasks[0].sourceFindingCode")
                        .value("BUSINESS_RULE_WITHOUT_SCENARIO"))
                .andExpect(jsonPath("$.tasks[0].targetNodeId").value("BR-002"))
                .andExpect(jsonPath("$.tasks[0].targetNodeType")
                        .value("BUSINESS_RULE"))
                .andExpect(jsonPath("$.tasks[0].description")
                        .value("Create a scenario that covers business rule BR-002."))
                .andExpect(jsonPath("$.tasks[0].dependsOn", hasSize(0)))
                .andExpect(jsonPath("$.tasks[1].id")
                        .value("TASK-CREATE-TEST-IMPLEMENTATION-SC-002"))
                .andExpect(jsonPath("$.tasks[1].type")
                        .value("CREATE_TEST_IMPLEMENTATION"))
                .andExpect(jsonPath("$.tasks[1].description")
                        .value("Create a test implementation that validates scenario SC-002."))
                .andExpect(jsonPath("$.tasks[2].id")
                        .value("TASK-CREATE-CHECK-TC-001"))
                .andExpect(jsonPath("$.tasks[2].type").value("CREATE_CHECK"))
                .andExpect(jsonPath("$.tasks[2].targetNodeType")
                        .value("TEST_IMPLEMENTATION"))
                .andExpect(jsonPath("$.tasks[2].description")
                        .value("Create at least one check for test implementation TC-001."))
                .andExpect(jsonPath("$.sourceFindingsSummary.total").value(3))
                .andExpect(jsonPath("$.sourceFindingsSummary.high").value(1))
                .andExpect(jsonPath("$.sourceFindingsSummary.medium").value(2))
                .andExpect(jsonPath("$.sourceFindingsSummary.low").value(0))
                .andExpect(jsonPath("$.validation.valid").value(true));
    }

    @Test
    void fullyCoveredModelShouldReturnSuccessfulEmptyRoadmap()
            throws Exception {
        String modelId = registerModel(validModel());

        mockMvc.perform(get("/api/v1/models/{modelId}/roadmap", modelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planned").value(true))
                .andExpect(jsonPath("$.summary.totalTasks").value(0))
                .andExpect(jsonPath("$.summary.plannedTasks").value(0))
                .andExpect(jsonPath("$.tasks", hasSize(0)))
                .andExpect(jsonPath("$.validation.valid").value(true));
    }

    @Test
    void repeatedRequestShouldReturnIdenticalJson() throws Exception {
        String modelId = registerModel(mixedModel());

        assertEquals(roadmapJson(modelId), roadmapJson(modelId));
    }

    @Test
    void unknownAndInvalidModelsShouldReuseExistingContracts()
            throws Exception {
        mockMvc.perform(get("/api/v1/models/{modelId}/roadmap", "unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("MODEL_NOT_FOUND"));

        String invalidId = repository.save(objectMapper.readTree("{}"), 0)
                .modelId();
        mockMvc.perform(get("/api/v1/models/{modelId}/roadmap", invalidId))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.summary.errors")
                        .value(greaterThan(0)));
    }

    @Test
    void coverageFindingsAndAssessmentEndpointsShouldRemainUnchanged()
            throws Exception {
        String modelId = registerModel(validModel());

        mockMvc.perform(get("/api/v1/models/{modelId}/coverage", modelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metrics", hasSize(3)));
        mockMvc.perform(get("/api/v1/models/{modelId}/findings", modelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.findings", hasSize(0)));
        mockMvc.perform(get("/api/v1/models/{modelId}/assessment", modelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.health").value("PASS"))
                .andExpect(jsonPath("$.roadmap").doesNotExist());
    }

    private String roadmapJson(String modelId) throws Exception {
        return mockMvc.perform(
                        get("/api/v1/models/{modelId}/roadmap", modelId))
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
                    || "HAS_CHECK".equals(relationship.path("type").asText())) {
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
