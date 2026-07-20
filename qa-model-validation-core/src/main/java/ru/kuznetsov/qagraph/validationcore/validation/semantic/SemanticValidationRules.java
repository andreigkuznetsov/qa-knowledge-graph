package ru.kuznetsov.qagraph.validationcore.validation.semantic;

import java.util.List;

public final class SemanticValidationRules {

    private SemanticValidationRules() {
    }

    public static List<KnowledgeRule> defaults() {
        return List.of(
                new DuplicateNodeIdRule(),
                new SourceReferenceRule(),
                new RelationshipIntegrityRule(),
                new UnknownToNodeRule(),
                new RelationshipNotAllowedRule(),
                new TestStepOrderRule(),
                new BusinessOperationCoverageRule(),
                new ScenarioWithoutTestRule(),
                new BusinessRuleWithoutScenarioRule(),
                new TestCheckCoverageRule()
        );
    }
}
