package ru.kuznetsov.qagraph.validationcore.validation.semantic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SemanticValidationEngineTest {

    private final JsonNode model = new ObjectMapper().createObjectNode();

    @Test
    void executesEveryRuleAndPreservesRuleAndFindingOrder() {
        KnowledgeRule first = rule("FIRST", "FIRST-1", "FIRST-2");
        KnowledgeRule empty = rule("EMPTY");
        KnowledgeRule last = rule("LAST", "LAST-1");

        List<ValidationIssue> findings = new SemanticValidationEngine(
                List.of(first, empty, last)).validate(model);

        assertEquals(List.of("FIRST-1", "FIRST-2", "LAST-1"),
                findings.stream().map(ValidationIssue::code).toList());
    }

    @Test
    void emptyRuleCollectionProducesNoFindings() {
        assertEquals(List.of(),
                new SemanticValidationEngine(List.of()).validate(model));
    }

    @Test
    void unexpectedRuleExceptionIsPropagated() {
        KnowledgeRule failing = new KnowledgeRule() {
            @Override
            public String code() {
                return "FAIL";
            }

            @Override
            public List<ValidationIssue> evaluate(JsonNode ignored) {
                throw new IllegalStateException("unexpected");
            }
        };

        assertThrows(IllegalStateException.class,
                () -> new SemanticValidationEngine(List.of(failing))
                        .validate(model));
    }

    private KnowledgeRule rule(String ruleCode, String... findingCodes) {
        return new KnowledgeRule() {
            @Override
            public String code() {
                return ruleCode;
            }

            @Override
            public List<ValidationIssue> evaluate(JsonNode ignored) {
                return java.util.Arrays.stream(findingCodes)
                        .map(code -> ValidationIssue.semanticError(
                                code, code, code))
                        .toList();
            }
        };
    }
}
