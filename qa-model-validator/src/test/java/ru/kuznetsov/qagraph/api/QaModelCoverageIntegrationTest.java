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

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class QaModelCoverageIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnMixedCoverageInStableOrder() throws Exception {
        String modelId = registerModel(mixedCoverageModel());

        mockMvc.perform(get("/api/v1/models/{modelId}/coverage", modelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelId").value(modelId))
                .andExpect(jsonPath("$.analyzed").value(true))
                .andExpect(jsonPath("$.schemaVersion").value("0.1"))
                .andExpect(jsonPath("$.metrics", hasSize(3)))
                .andExpect(jsonPath("$.metrics[0].metric")
                        .value("RULE_SCENARIO_COVERAGE"))
                .andExpect(jsonPath("$.metrics[1].metric")
                        .value("SCENARIO_TEST_COVERAGE"))
                .andExpect(jsonPath("$.metrics[2].metric")
                        .value("TEST_CHECK_COVERAGE"))
                .andExpect(jsonPath("$.metrics[0].total").value(2))
                .andExpect(jsonPath("$.metrics[0].covered").value(1))
                .andExpect(jsonPath("$.metrics[0].uncovered").value(1))
                .andExpect(jsonPath("$.metrics[0].coveragePercent").value(50.0))
                .andExpect(jsonPath("$.metrics[1].coveragePercent").value(50.0))
                .andExpect(jsonPath("$.metrics[2].coveragePercent").value(50.0));
    }

    @Test
    void shouldReturnZeroForEmptyCoverageCategories() throws Exception {
        ObjectNode model = (ObjectNode) objectMapper.readTree(validModel());
        model.set("nodes", objectMapper.createArrayNode());
        model.set("relationships", objectMapper.createArrayNode());
        String modelId = registerModel(model.toString());

        mockMvc.perform(get("/api/v1/models/{modelId}/coverage", modelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metrics", hasSize(3)))
                .andExpect(jsonPath("$.metrics[0].total").value(0))
                .andExpect(jsonPath("$.metrics[0].coveragePercent").value(0.0))
                .andExpect(jsonPath("$.metrics[1].coveragePercent").value(0.0))
                .andExpect(jsonPath("$.metrics[2].coveragePercent").value(0.0));
    }

    @Test
    void shouldReturnExistingErrorForUnknownModel() throws Exception {
        mockMvc.perform(get("/api/v1/models/{modelId}/coverage", "unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("MODEL_NOT_FOUND"));
    }

    @Test
    void shouldBeRepeatableAndLeaveExistingEndpointsUnaffected()
            throws Exception {
        String model = validModel();
        String modelId = registerModel(model);

        String first = mockMvc.perform(
                        get("/api/v1/models/{modelId}/coverage", modelId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(
                        get("/api/v1/models/{modelId}/coverage", modelId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertEquals(first, second);
        mockMvc.perform(get("/api/v1/models"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/models/{modelId}", modelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.project.id").value("DPD-TERMINAL"));
        mockMvc.perform(get("/api/v1/models/{modelId}/info", modelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelId").value(modelId));
        mockMvc.perform(get("/api/v1/models/{modelId}/trace", modelId)
                        .queryParam("from", "SC-001")
                        .queryParam("to", "BR-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true));
        mockMvc.perform(post("/api/v1/qa-model/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(model))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    private String mixedCoverageModel() throws Exception {
        ObjectNode model = (ObjectNode) objectMapper.readTree(validModel());
        ArrayNode nodes = (ArrayNode) model.path("nodes");
        addUncoveredCopy(nodes, "BR-001", "BR-002");
        addUncoveredCopy(nodes, "SC-001", "SC-002");
        addUncoveredCopy(nodes, "TC-001", "TC-002");
        for (JsonNode relationship : model.path("relationships")) {
            ObjectNode mutableRelationship = (ObjectNode) relationship;
            if ("COVERS".equals(relationship.path("type").asText())) {
                mutableRelationship.put("from", "SC-002");
            }
            if ("HAS_CHECK".equals(relationship.path("type").asText())) {
                mutableRelationship.put("from", "TC-002");
            }
        }
        return model.toString();
    }

    private void addUncoveredCopy(
            ArrayNode nodes,
            String sourceId,
            String targetId
    ) {
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
