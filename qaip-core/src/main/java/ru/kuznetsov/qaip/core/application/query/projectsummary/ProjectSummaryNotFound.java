package ru.kuznetsov.qaip.core.application.query.projectsummary;

import java.util.Objects;

public record ProjectSummaryNotFound(String projectId) implements ProjectSummaryQueryResult {
    public ProjectSummaryNotFound {
        Objects.requireNonNull(projectId, "projectId");
        if (projectId.isBlank()) throw new IllegalArgumentException("projectId must not be blank");
    }
}
