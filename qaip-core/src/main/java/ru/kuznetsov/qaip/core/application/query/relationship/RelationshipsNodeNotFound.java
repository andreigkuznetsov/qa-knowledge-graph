package ru.kuznetsov.qaip.core.application.query.relationship;

import java.util.Objects;

public record RelationshipsNodeNotFound(String projectId, String nodeId) implements RelationshipsQueryResult {
    public RelationshipsNodeNotFound {
        projectId = requireId(projectId, "projectId");
        nodeId = requireId(nodeId, "nodeId");
    }

    private static String requireId(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
