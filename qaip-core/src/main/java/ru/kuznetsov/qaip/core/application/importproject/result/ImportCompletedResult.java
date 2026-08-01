package ru.kuznetsov.qaip.core.application.importproject.result;

import java.util.Objects;

public record ImportCompletedResult(
        String projectId,
        int nodeCount,
        int relationshipCount
) implements ImportResult {
    public ImportCompletedResult {
        Objects.requireNonNull(projectId, "projectId");
        if (projectId.isBlank()) throw new IllegalArgumentException("projectId must not be blank");
        if (nodeCount < 0) throw new IllegalArgumentException("nodeCount must not be negative");
        if (relationshipCount < 0) {
            throw new IllegalArgumentException("relationshipCount must not be negative");
        }
    }
}
