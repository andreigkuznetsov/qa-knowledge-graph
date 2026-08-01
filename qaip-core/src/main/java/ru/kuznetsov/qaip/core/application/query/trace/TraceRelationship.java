package ru.kuznetsov.qaip.core.application.query.trace;

import java.util.Objects;

public record TraceRelationship(
        String relationshipId,
        String fromNodeId,
        String toNodeId,
        String relationshipType
) {
    public TraceRelationship {
        relationshipId = requireNonBlank(relationshipId, "relationshipId");
        fromNodeId = requireNonBlank(fromNodeId, "fromNodeId");
        toNodeId = requireNonBlank(toNodeId, "toNodeId");
        Objects.requireNonNull(relationshipType, "relationshipType");
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
