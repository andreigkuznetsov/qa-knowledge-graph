package ru.kuznetsov.qagraph.api;

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

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("smoke")
@SpringBootTest
@AutoConfigureMockMvc
class QaModelValidationSmokeTest {

    private static final String ENDPOINT =
            "/api/v1/qa-model/validate";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void validModelShouldReturnValidTrue() throws Exception {
        postResource("valid-qa-model.json")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.summary.errors").value(0));
    }

    @Test
    void invalidSchemaShouldReturnJsonSchemaError() throws Exception {
        postResource("invalid-schema-model.json")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath(
                        "$.issues[*].layer",
                        hasItem("JSON_SCHEMA")
                ))
                .andExpect(jsonPath(
                        "$.issues[*].layer",
                        not(hasItem("SEMANTIC"))
                ));
    }

    @Test
    void unknownTargetNodeShouldReturnUnknownToNode()
            throws Exception {

        postResource("unknown-node-model.json")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.summary.errors").value(1))
                .andExpect(jsonPath("$.issues[0].severity")
                        .value("ERROR"))
                .andExpect(jsonPath("$.issues[0].layer")
                        .value("SEMANTIC"))
                .andExpect(jsonPath("$.issues[0].code")
                        .value("UNKNOWN_TO_NODE"))
                .andExpect(jsonPath("$.issues[0].message")
                        .value("Связь указывает на отсутствующий "
                                + "to-узел: BO-404"))
                .andExpect(jsonPath("$.issues[0].objectId")
                        .value("REL-001"))
                .andExpect(jsonPath("$.issues[0].path")
                        .value("/relationships/0/to"));
    }

    @Test
    void invalidRelationshipShouldReturnRelationshipNotAllowed()
            throws Exception {

        postResource("invalid-relationship-model.json")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath(
                        "$.issues[*].code",
                        hasItem("RELATIONSHIP_NOT_ALLOWED")
                ))
                .andExpect(jsonPath(
                        "$.issues[?(@.code == 'RELATIONSHIP_NOT_ALLOWED')]"
                                + ".message",
                        hasItem("Недопустимая связь: BUSINESS_OPERATION "
                                + "--DESCRIBES--> USER_STORY")
                ));
    }

    @Test
    void uncoveredScenarioShouldReturnWarningAndRemainValid()
            throws Exception {

        postResource("scenario-without-test.json")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.summary.errors").value(0))
                .andExpect(jsonPath(
                        "$.summary.warnings",
                        greaterThan(0)
                ))
                .andExpect(jsonPath(
                        "$.issues[*].code",
                        hasItem("SCENARIO_WITHOUT_TEST")
                ))
                .andExpect(jsonPath(
                        "$.issues[?(@.code == 'SCENARIO_WITHOUT_TEST')]"
                                + ".message",
                        hasItem("BDD-сценарий не покрыт тестовой "
                                + "реализацией")
                ))
                .andExpect(jsonPath("$.issues.length()",
                        greaterThan(1)))
                .andExpect(jsonPath("$.issues[*].code", contains(
                        "CONFIRMED_WITHOUT_SOURCE",
                        "CONFIRMED_WITHOUT_SOURCE",
                        "CONFIRMED_WITHOUT_SOURCE",
                        "SCENARIO_WITHOUT_TEST",
                        "CONFIRMED_WITHOUT_SOURCE",
                        "CONFIRMED_WITHOUT_SOURCE"
                )))
                .andExpect(jsonPath("$.issues[*].objectId", contains(
                        "BO-001",
                        "CHK-001",
                        "SC-001",
                        "SC-001",
                        "TC-001",
                        "TI-UI-001"
                )));
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
                .andExpect(jsonPath("$.error")
                        .value("INVALID_JSON"));
    }

    private ResultActions postResource(String name)
            throws Exception {

        String request = new ClassPathResource(name)
                .getContentAsString(StandardCharsets.UTF_8);

        return mockMvc.perform(post(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request));
    }
}
