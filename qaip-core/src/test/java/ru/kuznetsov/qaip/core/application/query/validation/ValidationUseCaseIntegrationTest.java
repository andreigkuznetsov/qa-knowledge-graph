package ru.kuznetsov.qaip.core.application.query.validation;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.validation.ValidationEngine;
import ru.kuznetsov.qaip.core.application.validation.rule.IsolatedNodeValidationRule;
import ru.kuznetsov.qaip.core.application.validation.rule.ScenarioWithoutTestValidationRule;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.persistence.memory.InMemoryProjectReader;
import ru.kuznetsov.qaip.core.persistence.memory.InMemoryProjectRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static ru.kuznetsov.qaip.core.application.query.validation.DefaultValidationUseCaseTest.*;

class ValidationUseCaseIntegrationTest {
    @Test
    void real_in_memory_default_rule_flow_covers_all_outcomes_order_and_immutability() {
        Project clean = project("CLEAN", List.of(node("S", "SCENARIO"), node("T", "TEST_IMPLEMENTATION")),
                List.of(relationship("R", "T", "VALIDATES", "S")));
        Project warning = project("WARNING", List.of(node("I", "BUSINESS_RULE")), List.of());
        Project error = project("ERROR", List.of(node("S", "SCENARIO"), node("N", "BUSINESS_RULE")),
                List.of(relationship("R", "N", "RELATED_TO", "S")));
        Project both = project("BOTH", List.of(node("S", "SCENARIO"), node("I", "BUSINESS_RULE")), List.of());
        Project before = both;
        InMemoryProjectRepository repository = new InMemoryProjectRepository();
        for (Project project : List.of(clean, warning, error, both)) repository.insertIfAbsent(project);
        ValidationUseCase useCase = useCase(new InMemoryProjectReader(repository), new ValidationEngine(List.of(
                new IsolatedNodeValidationRule(), new ScenarioWithoutTestValidationRule())));

        ValidationCompleted cleanResult = completed(useCase, "CLEAN");
        assertTrue(cleanResult.report().valid());
        assertTrue(cleanResult.report().issues().isEmpty());
        ValidationCompleted warningResult = completed(useCase, "WARNING");
        assertTrue(warningResult.report().valid());
        assertEquals(1, warningResult.report().warningCount());
        ValidationCompleted errorResult = completed(useCase, "ERROR");
        assertFalse(errorResult.report().valid());
        assertEquals(1, errorResult.report().errorCount());
        ValidationCompleted bothResult = completed(useCase, "BOTH");
        assertEquals(List.of("ISOLATED_NODE", "ISOLATED_NODE", "SCENARIO_REQUIRES_TEST"),
                bothResult.report().issues().stream().map(ValidationIssueResult::ruleId).toList());
        assertEquals(bothResult, useCase.execute("BOTH"));
        assertEquals(new ValidationProjectNotFound("MISSING"), useCase.execute("MISSING"));
        assertEquals(before, both);
    }

    private static ValidationCompleted completed(ValidationUseCase useCase, String id) {
        return assertInstanceOf(ValidationCompleted.class, useCase.execute(id));
    }
}
