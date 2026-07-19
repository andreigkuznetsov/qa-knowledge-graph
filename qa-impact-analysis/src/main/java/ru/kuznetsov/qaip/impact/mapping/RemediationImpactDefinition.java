package ru.kuznetsov.qaip.impact.mapping;

import ru.kuznetsov.qagraph.model.NodeType;
import ru.kuznetsov.qagraph.model.RelationshipType;
import ru.kuznetsov.qaip.findings.model.FindingCode;
import ru.kuznetsov.qaip.impact.model.ImpactChangeType;
import ru.kuznetsov.qaip.impact.model.RelationEndpointRole;
import ru.kuznetsov.qaip.impact.model.ResolutionExpectation;
import ru.kuznetsov.qaip.roadmap.model.RemediationTaskType;

import java.util.Objects;

public record RemediationImpactDefinition(
        RemediationTaskType taskType,
        FindingCode sourceFindingCode,
        NodeType affectedNodeType,
        NodeType requiredNodeType,
        RelationshipType requiredRelationshipType,
        RelationEndpointRole affectedNodeRelationRole,
        ImpactChangeType changeType,
        ResolutionExpectation resolutionExpectation
) {
    public RemediationImpactDefinition {
        Objects.requireNonNull(taskType, "taskType must not be null");
        Objects.requireNonNull(sourceFindingCode,
                "sourceFindingCode must not be null");
        Objects.requireNonNull(affectedNodeType,
                "affectedNodeType must not be null");
        Objects.requireNonNull(requiredNodeType,
                "requiredNodeType must not be null");
        Objects.requireNonNull(requiredRelationshipType,
                "requiredRelationshipType must not be null");
        Objects.requireNonNull(affectedNodeRelationRole,
                "affectedNodeRelationRole must not be null");
        Objects.requireNonNull(changeType, "changeType must not be null");
        Objects.requireNonNull(resolutionExpectation,
                "resolutionExpectation must not be null");
    }
}
