package ru.kuznetsov.qagraph.validationcore.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SemanticQaModelValidatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final SemanticQaModelValidator validator =
            new SemanticQaModelValidator();

    @Test
    void shouldDetectUnknownTargetNode() throws Exception {
        JsonNode document = objectMapper.readTree("""
                {
                  "sources": [],
                  "nodes": [
                    {
                      "id": "US-001",
                      "type": "USER_STORY",
                      "status": "DRAFT",
                      "sourceReferences": []
                    }
                  ],
                  "relationships": [
                    {
                      "id": "REL-001",
                      "from": "US-001",
                      "type": "DESCRIBES",
                      "to": "BO-404"
                    }
                  ]
                }
                """);

        List<ValidationIssue> issues =
                validator.validate(document);

        assertTrue(issues.stream()
                .anyMatch(issue ->
                        issue.code().equals("UNKNOWN_TO_NODE")
                ));
    }

    @Test
    void shouldWarnWhenScenarioHasNoTest() throws Exception {
        JsonNode document = objectMapper.readTree("""
                {
                  "sources": [],
                  "nodes": [
                    {
                      "id": "SC-001",
                      "type": "SCENARIO",
                      "status": "DRAFT",
                      "sourceReferences": []
                    }
                  ],
                  "relationships": []
                }
                """);

        List<ValidationIssue> issues =
                validator.validate(document);

        assertTrue(issues.stream()
                .anyMatch(issue ->
                        issue.code().equals("SCENARIO_WITHOUT_TEST")
                ));
    }

    @Test
    void repeatedValidationShouldProduceEquivalentFindings()
            throws Exception {
        JsonNode document = objectMapper.readTree("""
                {
                  "sources": [],
                  "nodes": [
                    {"id":"SC-002","type":"SCENARIO","status":"DRAFT",
                     "sourceReferences":[]},
                    {"id":"SC-001","type":"SCENARIO","status":"DRAFT",
                     "sourceReferences":[]}
                  ],
                  "relationships": []
                }
                """);

        List<ValidationIssue> first = validator.validate(document);
        List<ValidationIssue> second = validator.validate(document);

        assertEquals(first, second);
        assertEquals(List.of("SC-002", "SC-001"), first.stream()
                .map(ValidationIssue::objectId)
                .toList());
        assertTrue(first.stream().allMatch(issue ->
                issue.code().equals("SCENARIO_WITHOUT_TEST")));
    }
}
