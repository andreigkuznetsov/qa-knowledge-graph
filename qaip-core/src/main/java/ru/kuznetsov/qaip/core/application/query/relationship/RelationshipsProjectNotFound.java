package ru.kuznetsov.qaip.core.application.query.relationship;

import java.util.Objects;

public record RelationshipsProjectNotFound(String projectId) implements RelationshipsQueryResult {
    public RelationshipsProjectNotFound {
        Objects.requireNonNull(projectId, "projectId");
        if (projectId.isBlank()) throw new IllegalArgumentException("projectId must not be blank");
    }
}
