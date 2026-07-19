package ru.kuznetsov.qaip.impact.model;

import ru.kuznetsov.qagraph.model.NodeType;
import ru.kuznetsov.qagraph.model.RelationshipType;

import java.util.Objects;

public record ExpectedStructuralChange(
        ImpactChangeType changeType,
        NodeType nodeTypeToCreate,
        RelationshipType relationTypeToCreate,
        String existingNodeId,
        RelationEndpointRole existingNodeRelationRole,
        ResolutionExpectation resolutionExpectation
) {
    public ExpectedStructuralChange {
        Objects.requireNonNull(changeType, "changeType must not be null");
        Objects.requireNonNull(nodeTypeToCreate,
                "nodeTypeToCreate must not be null");
        Objects.requireNonNull(relationTypeToCreate,
                "relationTypeToCreate must not be null");
        Objects.requireNonNull(existingNodeId,
                "existingNodeId must not be null");
        Objects.requireNonNull(existingNodeRelationRole,
                "existingNodeRelationRole must not be null");
        Objects.requireNonNull(resolutionExpectation,
                "resolutionExpectation must not be null");
    }
}
