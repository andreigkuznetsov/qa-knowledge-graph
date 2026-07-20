package ru.kuznetsov.qaip.simulation.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SimulationApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void successfulSimulationShouldReturnUnmappedDomainResult()
            throws Exception {
        mockMvc.perform(post("/api/v1/simulation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(sampleRequest().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.futureModelFingerprint").isNotEmpty())
                .andExpect(jsonPath("$.validation.valid").value(true))
                .andExpect(jsonPath("$.appliedMaterializations.length()")
                        .value(1))
                .andExpect(jsonPath("$.appliedMaterializations[0].taskId")
                        .value("TASK-TEST"))
                .andExpect(jsonPath("$.futureModel.nodes.length()").value(5))
                .andExpect(jsonPath("$.futureModel.relationships.length()")
                        .value(5));
    }

    @Test
    void invalidCandidateShouldReturnStableUnprocessableError()
            throws Exception {
        ObjectNode request = (ObjectNode) sampleRequest();
        ObjectNode futureNode = (ObjectNode) request
                .path("taskMaterializationSet")
                .path("materializations").get(0).path("futureNode");
        futureNode.remove("testImplementation");

        mockMvc.perform(post("/api/v1/simulation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code")
                        .value("CANDIDATE_MODEL_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.detail")
                        .value("Candidate QA model failed validation"))
                .andExpect(jsonPath("$.validation.valid").value(false))
                .andExpect(jsonPath("$.validation.issues[0].code")
                        .isNotEmpty());
    }

    @Test
    void malformedJsonShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/simulation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JSON"));
    }

    @Test
    void unsupportedMediaTypeShouldReturn415() throws Exception {
        mockMvc.perform(post("/api/v1/simulation")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(sampleRequest().toString()))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void missingBodyShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/simulation")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JSON"));
    }

    private JsonNode sampleRequest() throws IOException {
        try (var input = getClass().getResourceAsStream(
                "/simulation-request.json")) {
            if (input == null) {
                throw new IllegalStateException(
                        "simulation-request.json is missing");
            }
            return objectMapper.readTree(input);
        }
    }
}
