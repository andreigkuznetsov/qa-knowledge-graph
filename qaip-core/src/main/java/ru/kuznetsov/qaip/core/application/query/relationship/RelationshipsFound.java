package ru.kuznetsov.qaip.core.application.query.relationship;

import java.util.Objects;

public record RelationshipsFound(
        String projectId,
        String nodeId,
        RelationshipDetailsResult relationships) implements RelationshipsQueryResult {

    public RelationshipsFound {
        projectId = requireId(projectId, "projectId");
        nodeId = requireId(nodeId, "nodeId");
        Objects.requireNonNull(relationships, "relationships");
    }

    private static String requireId(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
