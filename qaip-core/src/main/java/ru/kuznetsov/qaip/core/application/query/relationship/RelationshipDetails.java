package ru.kuznetsov.qaip.core.application.query.relationship;

import java.util.Objects;

public record RelationshipDetails(
        String relationshipId,
        String fromNodeId,
        String toNodeId,
        String relationshipType) {

    public RelationshipDetails {
        relationshipId = requireNonBlank(relationshipId, "relationshipId");
        fromNodeId = requireNonBlank(fromNodeId, "fromNodeId");
        toNodeId = requireNonBlank(toNodeId, "toNodeId");
        relationshipType = requireNonBlank(relationshipType, "relationshipType");
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
