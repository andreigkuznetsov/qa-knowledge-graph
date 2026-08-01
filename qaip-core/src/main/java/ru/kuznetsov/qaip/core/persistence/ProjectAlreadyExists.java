package ru.kuznetsov.qaip.core.persistence;

import java.util.Objects;

/**
 * Confirms that a project with the submitted project's canonical identifier already existed, so this
 * invocation did not store the submitted project and left the existing project unchanged.
 * {@code projectId} is exactly the submitted project's canonical {@code metadata().id()}.
 */
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
