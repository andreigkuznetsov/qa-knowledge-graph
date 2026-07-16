package ru.kuznetsov.qagraph.extractor.api;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("smoke")
@SpringBootTest
@AutoConfigureMockMvc
class QaModelExtractionSmokeTest {

    private static final String ENDPOINT = "/api/v1/qa-model/extract";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void validStoryInputShouldProduceQaModel() throws Exception {
        postResource("story-input-sort-385.json")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extracted").value(true))
                .andExpect(jsonPath("$.summary.errors").value(0))
                .andExpect(jsonPath("$.qaModel.schemaVersion").value("0.1"))
                .andExpect(jsonPath(
                        "$.qaModel.nodes[?(@.type == 'USER_STORY')]"
                ).exists())
                .andExpect(jsonPath(
                        "$.qaModel.nodes[?(@.type == 'BUSINESS_OPERATION')]"
                ).exists())
                .andExpect(jsonPath(
                        "$.qaModel.relationships[?(@.type == 'DESCRIBES')]"
                ).exists());
    }

    @Test
    void invalidStoryInputShouldReturnSchemaErrors() throws Exception {
        postResource("invalid-story-input.json")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extracted").value(false))
                .andExpect(jsonPath(
                        "$.issues[*].layer",
                        hasItem("STORY_INPUT_SCHEMA")
                ));
    }

    @Test
    void unknownReferencesShouldBecomeWarnings() throws Exception {
        postResource("story-input-unknown-refs.json")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extracted").value(true))
                .andExpect(jsonPath("$.summary.errors").value(0))
                .andExpect(jsonPath(
                        "$.issues[*].code",
                        hasItem("UNKNOWN_RULE_CODE")
                ))
                .andExpect(jsonPath(
                        "$.issues[*].code",
                        hasItem("UNKNOWN_TECHNICAL_IMPLEMENTATION")
                ));
    }

    @Test
    void malformedJsonShouldReturn400() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "schemaVersion": "0.1",
                                  "project":
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_JSON"));
    }

    private ResultActions postResource(String name) throws Exception {
        String request = new ClassPathResource(name)
                .getContentAsString(StandardCharsets.UTF_8);

        return mockMvc.perform(post(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request));
    }
}
