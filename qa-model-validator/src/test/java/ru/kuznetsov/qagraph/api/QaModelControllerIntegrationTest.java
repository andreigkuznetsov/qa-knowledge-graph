package ru.kuznetsov.qagraph.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class QaModelControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRegisterAndRetrieveValidModel() throws Exception {
        String model = validModel();

        var registration = mockMvc.perform(post("/api/v1/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(model))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        org.hamcrest.Matchers.matchesPattern(
                                "/api/v1/models/.+"
                        )
                ))
                .andExpect(jsonPath("$.modelId").isString())
                .andExpect(jsonPath("$.nodeCount").value(7))
                .andExpect(jsonPath("$.relationshipCount").value(8))
                .andExpect(jsonPath("$.warnings").isNumber())
                .andReturn();

        String modelId = objectMapper.readTree(
                        registration.getResponse().getContentAsString()
                )
                .path("modelId")
                .asText();

        mockMvc.perform(get("/api/v1/models/{modelId}", modelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemaVersion").value("0.1"))
                .andExpect(jsonPath("$.project.id")
                        .value("DPD-TERMINAL"))
                .andExpect(jsonPath("$.nodes.length()").value(7))
                .andExpect(jsonPath("$.relationships.length()")
                        .value(8));
    }

    @Test
    void shouldGenerateDifferentModelIdsForRegistrations()
            throws Exception {
        String firstId = register(validModel());
        String secondId = register(validModel());

        assertNotEquals(firstId, secondId);
    }

    @Test
    void shouldRejectInvalidModelWithoutRegistration()
            throws Exception {
        mockMvc.perform(post("/api/v1/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"schemaVersion\":\"0.1\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.summary.errors")
                        .isNumber());
    }

    @Test
    void shouldReturn404ForUnknownModelId() throws Exception {
        mockMvc.perform(get("/api/v1/models/{modelId}", "unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("MODEL_NOT_FOUND"));
    }

    @Test
    void existingValidateEndpointShouldRemainUnchanged()
            throws Exception {
        mockMvc.perform(post("/api/v1/qa-model/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validModel()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.summary.errors").value(0));
    }

    private String register(String model) throws Exception {
        String response = mockMvc.perform(post("/api/v1/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(model))
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
