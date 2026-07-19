package ru.kuznetsov.qagraph.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class QaModelTraceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRegisterModelAndReturnShortestTrace() throws Exception {
        String modelId = registerModel();

        mockMvc.perform(get("/api/v1/models/{modelId}/trace", modelId)
                        .queryParam("from", "US-127")
                        .queryParam("to", "BR-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelId").value(modelId))
                .andExpect(jsonPath("$.fromNodeId").value("US-127"))
                .andExpect(jsonPath("$.toNodeId").value("BR-001"))
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.relationshipCount").value(2))
                .andExpect(jsonPath("$.path", hasSize(5)))
                .andExpect(jsonPath("$.path[0].nodeId")
                        .value("US-127"))
                .andExpect(jsonPath("$.path[1].relationshipType")
                        .value("DESCRIBES"))
                .andExpect(jsonPath("$.path[2].nodeId")
                        .value("BO-001"))
                .andExpect(jsonPath("$.path[3].relationshipType")
                        .value("GOVERNED_BY"))
                .andExpect(jsonPath("$.path[4].nodeId")
                        .value("BR-001"));
    }

    @Test
    void shouldReturnFoundFalseForMissingDirectedPath()
            throws Exception {
        String modelId = registerModel();

        mockMvc.perform(get("/api/v1/models/{modelId}/trace", modelId)
                        .queryParam("from", "BR-001")
                        .queryParam("to", "US-127"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(false))
                .andExpect(jsonPath("$.relationshipCount").value(0))
                .andExpect(jsonPath("$.path", hasSize(0)));
    }

    @Test
    void shouldReturn404ForUnknownModel() throws Exception {
        mockMvc.perform(get("/api/v1/models/{modelId}/trace", "unknown")
                        .queryParam("from", "A")
                        .queryParam("to", "B"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("MODEL_NOT_FOUND"));
    }

    @Test
    void shouldReturn404ForUnknownFromNode() throws Exception {
        String modelId = registerModel();

        mockMvc.perform(get("/api/v1/models/{modelId}/trace", modelId)
                        .queryParam("from", "UNKNOWN")
                        .queryParam("to", "BR-001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("NODE_NOT_FOUND"))
                .andExpect(jsonPath("$.nodeId").value("UNKNOWN"));
    }

    @Test
    void shouldReturn404ForUnknownToNode() throws Exception {
        String modelId = registerModel();

        mockMvc.perform(get("/api/v1/models/{modelId}/trace", modelId)
                        .queryParam("from", "US-127")
                        .queryParam("to", "UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("NODE_NOT_FOUND"))
                .andExpect(jsonPath("$.nodeId").value("UNKNOWN"));
    }

    @Test
    void shouldReturnUnifiedErrorForMissingOrBlankParameter()
            throws Exception {
        String modelId = registerModel();

        mockMvc.perform(get("/api/v1/models/{modelId}/trace", modelId)
                        .queryParam("to", "BR-001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("INVALID_REQUEST_PARAMETER"))
                .andExpect(jsonPath("$.parameter").value("from"));

        mockMvc.perform(get("/api/v1/models/{modelId}/trace", modelId)
                        .queryParam("from", "US-127")
                        .queryParam("to", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("INVALID_REQUEST_PARAMETER"))
                .andExpect(jsonPath("$.parameter").value("to"));
    }

    @Test
    void existingRegistrationRetrievalAndValidationShouldStillWork()
            throws Exception {
        String modelId = registerModel();

        mockMvc.perform(get("/api/v1/models/{modelId}", modelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.project.id")
                        .value("DPD-TERMINAL"));

        mockMvc.perform(post("/api/v1/qa-model/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validModel()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    private String registerModel() throws Exception {
        String response = mockMvc.perform(post("/api/v1/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validModel()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode responseJson = objectMapper.readTree(response);
        return responseJson.path("modelId").asText();
    }

    private String validModel() throws Exception {
        return new ClassPathResource("valid-qa-model.json")
                .getContentAsString(StandardCharsets.UTF_8);
    }
}
