package ru.kuznetsov.qaip.impact.mapping;

import ru.kuznetsov.qagraph.model.NodeType;
import ru.kuznetsov.qagraph.model.RelationshipType;
import ru.kuznetsov.qaip.findings.model.FindingCode;
import ru.kuznetsov.qaip.impact.model.ImpactChangeType;
import ru.kuznetsov.qaip.impact.model.RelationEndpointRole;
import ru.kuznetsov.qaip.impact.model.ResolutionExpectation;
import ru.kuznetsov.qaip.roadmap.model.RemediationTaskType;

import java.util.Map;

public final class RemediationImpactCatalog {

    private static final ResolutionExpectation EXPECTED_RESOLUTION =
            ResolutionExpectation
                    .FINDING_EXPECTED_TO_BE_RESOLVED_AFTER_VALID_COMPLETION;

    private static final Map<RemediationTaskType, RemediationImpactDefinition>
            DEFINITIONS = Map.of(
                    RemediationTaskType.CREATE_SCENARIO,
                    definition(
                            RemediationTaskType.CREATE_SCENARIO,
                            FindingCode.BUSINESS_RULE_WITHOUT_SCENARIO,
                            NodeType.BUSINESS_RULE,
                            NodeType.SCENARIO,
                            RelationshipType.COVERS,
                            RelationEndpointRole.TARGET
                    ),
                    RemediationTaskType.CREATE_TEST_IMPLEMENTATION,
                    definition(
                            RemediationTaskType.CREATE_TEST_IMPLEMENTATION,
                            FindingCode.SCENARIO_WITHOUT_TEST,
                            NodeType.SCENARIO,
                            NodeType.TEST_IMPLEMENTATION,
                            RelationshipType.VALIDATES,
                            RelationEndpointRole.TARGET
                    ),
                    RemediationTaskType.CREATE_CHECK,
                    definition(
                            RemediationTaskType.CREATE_CHECK,
                            FindingCode.TEST_WITHOUT_CHECK,
                            NodeType.TEST_IMPLEMENTATION,
                            NodeType.CHECK,
                            RelationshipType.HAS_CHECK,
                            RelationEndpointRole.SOURCE
                    )
            );

    private RemediationImpactCatalog() {
    }

    public static Map<RemediationTaskType, RemediationImpactDefinition>
    definitions() {
        return DEFINITIONS;
    }

    public static RemediationImpactDefinition definitionFor(
            RemediationTaskType taskType
    ) {
        return DEFINITIONS.get(taskType);
    }

    private static RemediationImpactDefinition definition(
            RemediationTaskType taskType,
            FindingCode findingCode,
            NodeType affectedNodeType,
            NodeType requiredNodeType,
            RelationshipType relationshipType,
            RelationEndpointRole affectedNodeRelationRole
    ) {
        return new RemediationImpactDefinition(
                taskType,
                findingCode,
                affectedNodeType,
                requiredNodeType,
                relationshipType,
                affectedNodeRelationRole,
                ImpactChangeType.CREATE_RELATED_NODE,
                EXPECTED_RESOLUTION
        );
    }
}
