package ru.kuznetsov.qagraph.validationcore.validation.semantic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticRulesTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void duplicateNodeIdRuleReportsViolationsInNodeOrder() throws Exception {
        JsonNode model = model("""
                {"id":"N-1","type":"CHECK"},
                {"id":"N-1","type":"CHECK"},
                {"id":"N-1","type":"CHECK"}
                """, "");

        List<ValidationIssue> findings = new DuplicateNodeIdRule()
                .evaluate(model);

        assertEquals(List.of("DUPLICATE_NODE_ID", "DUPLICATE_NODE_ID"),
                codes(findings));
        assertEquals("Обнаружен повторяющийся node.id: N-1",
                findings.getFirst().message());
    }

    @Test
    void sourceReferenceRulePreservesBothExistingChecks() throws Exception {
        JsonNode model = objectMapper.readTree("""
                {"sources":[],"nodes":[
                  {"id":"N-1","status":"CONFIRMED","sourceReferences":[]},
                  {"id":"N-2","status":"DRAFT","sourceReferences":[
                    {"sourceId":"SRC-404"}, {"sourceId":"SRC-405"}]}
                ],"relationships":[]}
                """);

        List<ValidationIssue> findings = new SourceReferenceRule()
                .evaluate(model);

        assertEquals(List.of("CONFIRMED_WITHOUT_SOURCE",
                "UNKNOWN_SOURCE_REFERENCE", "UNKNOWN_SOURCE_REFERENCE"),
                codes(findings));
    }

    @Test
    void relationshipIntegrityRulePreservesStructuralChecks() throws Exception {
        JsonNode model = model(
                "{\"id\":\"SC-1\",\"type\":\"SCENARIO\"}",
                """
                {"id":"R-1","from":"MISSING","type":"REFINES","to":"SC-1"},
                {"id":"R-1","from":"SC-1","type":"REFINES","to":"SC-1"},
                {"id":"R-2","from":"SC-1","type":"REFINES","to":"SC-1"}
                """);

        assertEquals(List.of("UNKNOWN_FROM_NODE",
                        "DUPLICATE_RELATIONSHIP_ID",
                        "SELF_REFERENCE_NOT_ALLOWED",
                        "SELF_REFERENCE_NOT_ALLOWED",
                        "DUPLICATE_RELATIONSHIP"),
                codes(new RelationshipIntegrityRule().evaluate(model)));
    }

    @Test
    void unknownToNodeRuleReportsExactStableFinding() throws Exception {
        JsonNode model = model(
                "{\"id\":\"US-1\",\"type\":\"USER_STORY\"}",
                """
                {"id":"R-1","from":"US-1","type":"DESCRIBES","to":"BO-404"},
                {"id":"R-2","from":"US-1","type":"DESCRIBES","to":"BO-405"}
                """);

        List<ValidationIssue> findings = new UnknownToNodeRule()
                .evaluate(model);

        assertEquals(List.of("UNKNOWN_TO_NODE", "UNKNOWN_TO_NODE"),
                codes(findings));
        assertEquals("Связь указывает на отсутствующий to-узел: BO-404",
                findings.getFirst().message());
        assertEquals("/relationships/0/to", findings.getFirst().path());
    }

    @Test
    void relationshipNotAllowedRuleReportsOnlyResolvedViolations()
            throws Exception {
        JsonNode model = model("""
                {"id":"US-1","type":"USER_STORY"},
                {"id":"BO-1","type":"BUSINESS_OPERATION"}
                """, """
                {"id":"OK","from":"US-1","type":"DESCRIBES","to":"BO-1"},
                {"id":"BAD","from":"BO-1","type":"DESCRIBES","to":"US-1"}
                """);

        List<ValidationIssue> findings = new RelationshipNotAllowedRule()
                .evaluate(model);

        assertEquals(List.of("RELATIONSHIP_NOT_ALLOWED"), codes(findings));
        assertEquals("Недопустимая связь: BUSINESS_OPERATION --DESCRIBES--> USER_STORY",
                findings.getFirst().message());
    }

    @Test
    void testStepOrderRuleReportsRepeatedOrdersInEncounterOrder()
            throws Exception {
        JsonNode model = model("""
                {"id":"T-1","type":"TEST_IMPLEMENTATION",
                 "testImplementation":{"steps":[
                   {"order":1},{"order":1},{"order":2},{"order":2}]}}
                """, "");

        assertEquals(List.of("DUPLICATE_TEST_STEP_ORDER",
                        "DUPLICATE_TEST_STEP_ORDER"),
                codes(new TestStepOrderRule().evaluate(model)));
    }

    @Test
    void businessOperationCoverageRuleReportsAllMissingAssociations()
            throws Exception {
        JsonNode model = model(
                "{\"id\":\"BO-1\",\"type\":\"BUSINESS_OPERATION\"}",
                "");

        assertEquals(List.of("OPERATION_WITHOUT_RULE",
                        "OPERATION_WITHOUT_SCENARIO",
                        "OPERATION_WITHOUT_IMPLEMENTATION",
                        "OPERATION_WITHOUT_STORY"),
                codes(new BusinessOperationCoverageRule().evaluate(model)));
    }

    @Test
    void scenarioWithoutTestRuleDistinguishesCoveredAndUncoveredScenarios()
            throws Exception {
        JsonNode model = model("""
                {"id":"SC-1","type":"SCENARIO"},
                {"id":"SC-2","type":"SCENARIO"},
                {"id":"T-1","type":"TEST_IMPLEMENTATION"}
                """, """
                {"id":"R-1","from":"T-1","type":"VALIDATES","to":"SC-1"}
                """);

        List<ValidationIssue> findings = new ScenarioWithoutTestRule()
                .evaluate(model);

        assertEquals(List.of("SCENARIO_WITHOUT_TEST"), codes(findings));
        assertEquals("SC-2", findings.getFirst().objectId());
        assertEquals("BDD-сценарий не покрыт тестовой реализацией",
                findings.getFirst().message());
    }

    @Test
    void businessRuleWithoutScenarioRuleHandlesMultipleViolations()
            throws Exception {
        JsonNode model = model("""
                {"id":"BR-1","type":"BUSINESS_RULE"},
                {"id":"BR-2","type":"BUSINESS_RULE"}
                """, "");

        assertEquals(List.of("BR-1", "BR-2"),
                new BusinessRuleWithoutScenarioRule().evaluate(model)
                        .stream().map(ValidationIssue::objectId).toList());
    }

    @Test
    void testCheckCoverageRulePreservesTestAndCheckWarnings()
            throws Exception {
        JsonNode model = model("""
                {"id":"T-1","type":"TEST_IMPLEMENTATION"},
                {"id":"C-1","type":"CHECK"}
                """, "");

        assertEquals(List.of("TEST_WITHOUT_CHECK", "ORPHAN_CHECK"),
                codes(new TestCheckCoverageRule().evaluate(model)));
    }

    @Test
    void defaultRulesDoNotMutateInputAndReturnNoFindingsForEmptyModel()
            throws Exception {
        JsonNode model = model("", "");
        JsonNode snapshot = model.deepCopy();

        List<ValidationIssue> findings = new SemanticValidationEngine(
                SemanticValidationRules.defaults()).validate(model);

        assertTrue(findings.isEmpty());
        assertEquals(snapshot, model);
    }

    private JsonNode model(String nodes, String relationships)
            throws Exception {
        return objectMapper.readTree("""
                {"sources":[],"nodes":[%s],"relationships":[%s]}
                """.formatted(nodes, relationships));
    }

    private List<String> codes(List<ValidationIssue> findings) {
        return findings.stream().map(ValidationIssue::code).toList();
    }
}
