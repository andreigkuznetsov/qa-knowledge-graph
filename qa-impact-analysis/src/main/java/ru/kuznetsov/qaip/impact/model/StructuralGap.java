package ru.kuznetsov.qaip.impact.model;

import ru.kuznetsov.qagraph.model.NodeType;
import ru.kuznetsov.qagraph.model.RelationshipType;

import java.util.Objects;

public record StructuralGap(
        NodeType affectedNodeType,
        NodeType requiredNodeType,
        RelationshipType requiredRelationshipType,
        RelationEndpointRole affectedNodeRelationRole
) {
    public StructuralGap {
        Objects.requireNonNull(affectedNodeType,
                "affectedNodeType must not be null");
        Objects.requireNonNull(requiredNodeType,
                "requiredNodeType must not be null");
        Objects.requireNonNull(requiredRelationshipType,
                "requiredRelationshipType must not be null");
        Objects.requireNonNull(affectedNodeRelationRole,
                "affectedNodeRelationRole must not be null");
    }
}
