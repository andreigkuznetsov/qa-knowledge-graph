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
class QaModelManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnEmptyModelList() throws Exception {
        mockMvc.perform(get("/api/v1/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldListOneRegisteredModel() throws Exception {
        String modelId = registerModel();

        mockMvc.perform(get("/api/v1/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].modelId").value(modelId))
                .andExpect(jsonPath("$[0].createdAt").isString())
                .andExpect(jsonPath("$[0].nodeCount").value(7))
                .andExpect(jsonPath("$[0].relationshipCount")
                        .value(8))
                .andExpect(jsonPath("$[0].warningCount")
                        .isNumber())
                .andExpect(jsonPath("$[0].project").doesNotExist())
                .andExpect(jsonPath("$[0].nodes").doesNotExist());
    }

    @Test
    void shouldListMultipleModelsNewestFirst() throws Exception {
        String firstId = registerModel();
        Thread.sleep(2);
        String secondId = registerModel();

        mockMvc.perform(get("/api/v1/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].modelId").value(secondId))
                .andExpect(jsonPath("$[1].modelId").value(firstId));
    }

    @Test
    void shouldReturnDescriptorById() throws Exception {
        String modelId = registerModel();

        mockMvc.perform(get(
                        "/api/v1/models/{modelId}/info",
                        modelId
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelId").value(modelId))
                .andExpect(jsonPath("$.createdAt").isString())
                .andExpect(jsonPath("$.nodeCount").value(7))
                .andExpect(jsonPath("$.relationshipCount").value(8))
                .andExpect(jsonPath("$.warningCount").isNumber());
    }

    @Test
    void shouldReturn404ForUnknownDescriptor() throws Exception {
        mockMvc.perform(get(
                        "/api/v1/models/{modelId}/info",
                        "unknown"
                ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("MODEL_NOT_FOUND"));
    }

    private String registerModel() throws Exception {
        String response = mockMvc.perform(post("/api/v1/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validModel()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdAt").doesNotExist())
                .andExpect(jsonPath("$.warningCount").doesNotExist())
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
