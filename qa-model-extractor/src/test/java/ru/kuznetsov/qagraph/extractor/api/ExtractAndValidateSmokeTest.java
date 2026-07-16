package ru.kuznetsov.qagraph.extractor.api;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("smoke")
@SpringBootTest
@AutoConfigureMockMvc
class ExtractAndValidateSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sort385ShouldReturnSemanticCoverageWarning()
            throws Exception {

        String request = new ClassPathResource(
                "story-input-sort-385.json"
        ).getContentAsString(StandardCharsets.UTF_8);

        mockMvc.perform(post(
                        "/api/v1/qa-model/extract-and-validate"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.extraction.extracted")
                        .value(true))
                .andExpect(jsonPath("$.validation.valid")
                        .value(true))
                .andExpect(jsonPath("$.validation.errors")
                        .value(0))
                .andExpect(jsonPath("$.validation.warnings")
                        .value(1))
                .andExpect(jsonPath(
                        "$.validation.issues[*].code",
                        hasItem("SCENARIO_WITHOUT_TEST")
                ))
                .andExpect(jsonPath(
                        "$.validation.issues[0].objectId"
                ).value("SC-001"));
    }
}
