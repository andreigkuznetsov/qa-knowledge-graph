package ru.kuznetsov.qagraph.validationcore.validation.semantic;

import java.util.List;

public final class SemanticValidationRules {

    private SemanticValidationRules() {
    }

    public static List<KnowledgeRule> defaults() {
        return List.of(
                new DuplicateNodeIdRule(),
                new ConfirmedWithoutSourceRule(),
                new UnknownSourceReferenceRule(),
                new DuplicateRelationshipIdRule(),
                new UnknownFromNodeRule(),
                new UnknownToNodeRule(),
                new RelationshipNotAllowedRule(),
                new SelfReferenceNotAllowedRule(),
                new DuplicateRelationshipRule(),
                new TestStepOrderRule(),
                new OperationWithoutRuleRule(),
                new OperationWithoutScenarioRule(),
                new OperationWithoutImplementationRule(),
                new OperationWithoutStoryRule(),
                new ScenarioWithoutTestRule(),
                new BusinessRuleWithoutScenarioRule(),
                new TestWithoutCheckRule(),
                new OrphanCheckRule()
        );
    }
}
