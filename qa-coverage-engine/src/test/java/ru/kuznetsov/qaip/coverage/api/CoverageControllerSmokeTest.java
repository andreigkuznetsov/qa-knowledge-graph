package ru.kuznetsov.qaip.coverage.api;

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
class CoverageControllerSmokeTest {

    private static final String ENDPOINT =
            "/api/v1/coverage";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void partialCheckCoverageShouldReturnProblem()
            throws Exception {

        postFixture("check-coverage-partial.json")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analyzed")
                        .value(true))
                .andExpect(jsonPath("$.release")
                        .value("0.4"))
                .andExpect(jsonPath(
                        "$.summary.ruleCoveragePercentage"
                ).value(100.0))
                .andExpect(jsonPath(
                        "$.summary.scenarioCoveragePercentage"
                ).value(100.0))
                .andExpect(jsonPath(
                        "$.summary.totalTests"
                ).value(2))
                .andExpect(jsonPath(
                        "$.summary.coveredTests"
                ).value(1))
                .andExpect(jsonPath(
                        "$.summary.uncoveredTests"
                ).value(1))
                .andExpect(jsonPath(
                        "$.summary.checkCoveragePercentage"
                ).value(50.0))
                .andExpect(jsonPath(
                        "$.problems[*].type",
                        hasItem("MISSING_CHECK")
                ))
                .andExpect(jsonPath(
                        "$.problems[0].objectId"
                ).value("TEST-002"))
                .andExpect(jsonPath(
                        "$.problems[0].path"
                ).value("/nodes/8"));
    }

    @Test
    void invalidQaModelShouldNotBeAnalyzed()
            throws Exception {

        postFixture("invalid-qa-model.json")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analyzed")
                        .value(false))
                .andExpect(jsonPath(
                        "$.validation.valid"
                ).value(false));
    }

    @Test
    void malformedJsonShouldReturn400()
            throws Exception {

        mockMvc.perform(post(ENDPOINT)
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("INVALID_JSON"));
    }

    private org.springframework.test.web.servlet.ResultActions
    postFixture(String name) throws Exception {

        String request = new ClassPathResource(name)
                .getContentAsString(
                        StandardCharsets.UTF_8
                );

        return mockMvc.perform(post(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request));
    }
}
