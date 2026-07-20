package ru.kuznetsov.qagraph.validationcore.validation.semantic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SemanticRulesTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ParameterizedTest(name = "{0}")
    @MethodSource("ruleCases")
    void eachRuleHasExactSingleViolationCharacterization(
            String name,
            KnowledgeRule rule,
            JsonNode model,
            ValidationIssue expected
    ) {
        List<ValidationIssue> findings = rule.evaluate(model);

        assertEquals(rule.code(), expected.code());
        assertEquals(List.of(expected), findings);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allRules")
    void eachRuleReturnsNoFindingForEmptyValidInput(
            String name,
            KnowledgeRule rule
    ) throws Exception {
        assertTrue(rule.evaluate(model("", "")).isEmpty());
    }

    @Test
    void repeatedViolationsPreserveModelOrder() throws Exception {
        JsonNode model = model("""
                {"id":"SC-1","type":"SCENARIO"},
                {"id":"SC-2","type":"SCENARIO"}
                """, "");

        assertEquals(List.of("SC-1", "SC-2"),
                new ScenarioWithoutTestRule().evaluate(model).stream()
                        .map(ValidationIssue::objectId)
                        .toList());
    }

    @Test
    void unrelatedRelationshipDoesNotSatisfyOperationInvariant()
            throws Exception {
        JsonNode model = model("""
                {"id":"BO-1","type":"BUSINESS_OPERATION"},
                {"id":"BR-1","type":"BUSINESS_RULE"}
                """, """
                {"id":"R-1","from":"BO-1","type":"RELATED_TO","to":"BO-1"}
                """);

        assertEquals(1, new OperationWithoutRuleRule()
                .evaluate(model).size());
    }

    @Test
    void defaultRulesDoNotMutateInput() throws Exception {
        JsonNode model = model("""
                {"id":"SC-1","type":"SCENARIO"}
                """, "");
        JsonNode snapshot = model.deepCopy();

        new SemanticValidationEngine(SemanticValidationRules.defaults())
                .validate(model);

        assertEquals(snapshot, model);
    }

    private Stream<Arguments> ruleCases() throws Exception {
        return Stream.of(
                arguments("duplicate node ID", new DuplicateNodeIdRule(),
                        model("""
                                {"id":"N-1","type":"CHECK"},
                                {"id":"N-1","type":"CHECK"}
                                """, ""),
                        ValidationIssue.semanticError("DUPLICATE_NODE_ID",
                                "Обнаружен повторяющийся node.id: N-1", "N-1")),
                arguments("confirmed without source",
                        new ConfirmedWithoutSourceRule(),
                        objectMapper.readTree("""
                                {"sources":[],"nodes":[
                                  {"id":"N-1","status":"CONFIRMED",
                                   "sourceReferences":[]}],"relationships":[]}
                                """),
                        ValidationIssue.semanticWarning(
                                "CONFIRMED_WITHOUT_SOURCE",
                                "Подтверждённый узел не имеет ссылки на источник",
                                "N-1")),
                arguments("unknown source reference",
                        new UnknownSourceReferenceRule(),
                        objectMapper.readTree("""
                                {"sources":[],"nodes":[
                                  {"id":"N-1","sourceReferences":[
                                    {"sourceId":"SRC-404"}]}],"relationships":[]}
                                """),
                        ValidationIssue.semanticError(
                                "UNKNOWN_SOURCE_REFERENCE",
                                "Ссылка указывает на отсутствующий источник: SRC-404",
                                "N-1")),
                arguments("duplicate relationship ID",
                        new DuplicateRelationshipIdRule(),
                        model("", """
                                {"id":"R-1"},{"id":"R-1"}
                                """),
                        ValidationIssue.semanticError(
                                "DUPLICATE_RELATIONSHIP_ID",
                                "Обнаружен повторяющийся relationship.id: R-1",
                                "R-1", "/relationships/1/id")),
                arguments("unknown from node", new UnknownFromNodeRule(),
                        model("{\"id\":\"N-1\",\"type\":\"CHECK\"}", """
                                {"id":"R-1","from":"N-404","to":"N-1"}
                                """),
                        ValidationIssue.semanticError("UNKNOWN_FROM_NODE",
                                "Связь указывает на отсутствующий from-узел: N-404",
                                "R-1", "/relationships/0/from")),
                arguments("unknown to node", new UnknownToNodeRule(),
                        model("{\"id\":\"N-1\",\"type\":\"CHECK\"}", """
                                {"id":"R-1","from":"N-1","to":"N-404"}
                                """),
                        ValidationIssue.semanticError("UNKNOWN_TO_NODE",
                                "Связь указывает на отсутствующий to-узел: N-404",
                                "R-1", "/relationships/0/to")),
                arguments("relationship not allowed",
                        new RelationshipNotAllowedRule(),
                        model("""
                                {"id":"US-1","type":"USER_STORY"},
                                {"id":"BO-1","type":"BUSINESS_OPERATION"}
                                """, """
                                {"id":"R-1","from":"BO-1","type":"DESCRIBES","to":"US-1"}
                                """),
                        ValidationIssue.semanticError(
                                "RELATIONSHIP_NOT_ALLOWED",
                                "Недопустимая связь: BUSINESS_OPERATION --DESCRIBES--> USER_STORY",
                                "R-1", "/relationships/0/type")),
                arguments("self-reference not allowed",
                        new SelfReferenceNotAllowedRule(),
                        model("{\"id\":\"SC-1\",\"type\":\"SCENARIO\"}", """
                                {"id":"R-1","from":"SC-1","type":"REFINES","to":"SC-1"}
                                """),
                        ValidationIssue.semanticError(
                                "SELF_REFERENCE_NOT_ALLOWED",
                                "Самоссылка запрещена для связи REFINES",
                                "R-1", "/relationships/0")),
                arguments("duplicate relationship",
                        new DuplicateRelationshipRule(),
                        model("""
                                {"id":"US-1","type":"USER_STORY"},
                                {"id":"BO-1","type":"BUSINESS_OPERATION"}
                                """, """
                                {"id":"R-1","from":"US-1","type":"DESCRIBES","to":"BO-1"},
                                {"id":"R-2","from":"US-1","type":"DESCRIBES","to":"BO-1"}
                                """),
                        ValidationIssue.semanticError("DUPLICATE_RELATIONSHIP",
                                "Обнаружена дублирующая связь: US-1|DESCRIBES|BO-1",
                                "R-2", "/relationships/1")),
                arguments("duplicate test step order", new TestStepOrderRule(),
                        model("""
                                {"id":"T-1","type":"TEST_IMPLEMENTATION",
                                 "testImplementation":{"steps":[{"order":1},{"order":1}]}}
                                """, ""),
                        ValidationIssue.semanticError(
                                "DUPLICATE_TEST_STEP_ORDER",
                                "В тесте повторяется номер шага: 1", "T-1")),
                operationCase("operation without rule",
                        new OperationWithoutRuleRule(),
                        "OPERATION_WITHOUT_RULE",
                        "Бизнес-операция не связана ни с одним правилом"),
                operationCase("operation without scenario",
                        new OperationWithoutScenarioRule(),
                        "OPERATION_WITHOUT_SCENARIO",
                        "Бизнес-операция не связана ни с одним сценарием"),
                operationCase("operation without implementation",
                        new OperationWithoutImplementationRule(),
                        "OPERATION_WITHOUT_IMPLEMENTATION",
                        "Бизнес-операция не имеет технической реализации"),
                operationCase("operation without story",
                        new OperationWithoutStoryRule(),
                        "OPERATION_WITHOUT_STORY",
                        "Бизнес-операция не связана ни с одной User Story"),
                nodeWarningCase("scenario without test",
                        new ScenarioWithoutTestRule(), "SCENARIO",
                        "SCENARIO_WITHOUT_TEST",
                        "BDD-сценарий не покрыт тестовой реализацией"),
                nodeWarningCase("business rule without scenario",
                        new BusinessRuleWithoutScenarioRule(), "BUSINESS_RULE",
                        "RULE_WITHOUT_SCENARIO",
                        "Бизнес-правило не покрыто BDD-сценарием"),
                nodeWarningCase("test without check",
                        new TestWithoutCheckRule(), "TEST_IMPLEMENTATION",
                        "TEST_WITHOUT_CHECK",
                        "Тестовая реализация не содержит проверок"),
                nodeWarningCase("orphan check", new OrphanCheckRule(), "CHECK",
                        "ORPHAN_CHECK",
                        "Проверка не привязана к тестовой реализации")
        );
    }

    private Stream<Arguments> allRules() {
        return SemanticValidationRules.defaults().stream()
                .map(rule -> Arguments.of(rule.getClass().getSimpleName(), rule));
    }

    private Arguments operationCase(
            String name,
            KnowledgeRule rule,
            String code,
            String message
    ) throws Exception {
        return arguments(name, rule,
                model("{\"id\":\"BO-1\",\"type\":\"BUSINESS_OPERATION\"}", ""),
                ValidationIssue.semanticWarning(code, message, "BO-1"));
    }

    private Arguments nodeWarningCase(
            String name,
            KnowledgeRule rule,
            String type,
            String code,
            String message
    ) throws Exception {
        return arguments(name, rule,
                model("{\"id\":\"N-1\",\"type\":\"" + type + "\"}", ""),
                ValidationIssue.semanticWarning(code, message, "N-1"));
    }

    private Arguments arguments(
            String name,
            KnowledgeRule rule,
            JsonNode model,
            ValidationIssue issue
    ) {
        return Arguments.of(name, rule, model, issue);
    }

    private JsonNode model(String nodes, String relationships)
            throws Exception {
        return objectMapper.readTree("""
                {"sources":[],"nodes":[%s],"relationships":[%s]}
                """.formatted(nodes, relationships));
    }
}
