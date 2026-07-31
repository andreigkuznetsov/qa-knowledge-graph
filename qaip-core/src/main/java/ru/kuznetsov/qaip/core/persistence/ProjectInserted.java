package ru.kuznetsov.qaip.core.persistence;

import java.util.Objects;

public record ProjectInserted(String projectId) implements ProjectInsertResult {
    public ProjectInserted {
        projectId = requireProjectId(projectId);
    }

    private static String requireProjectId(String projectId) {
        Objects.requireNonNull(projectId, "projectId");
        if (projectId.isBlank()) throw new IllegalArgumentException("projectId must not be blank");
        return projectId;
    }
}
