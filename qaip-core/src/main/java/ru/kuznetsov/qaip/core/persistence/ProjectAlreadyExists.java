package ru.kuznetsov.qaip.core.persistence;

import java.util.Objects;

public record ProjectAlreadyExists(String projectId) implements ProjectInsertResult {
    public ProjectAlreadyExists {
        projectId = requireProjectId(projectId);
    }

    private static String requireProjectId(String projectId) {
        Objects.requireNonNull(projectId, "projectId");
        if (projectId.isBlank()) throw new IllegalArgumentException("projectId must not be blank");
        return projectId;
    }
}
