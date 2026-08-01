package ru.kuznetsov.qaip.core.application.query.validation;

import java.util.Objects;

public record ValidationProjectNotFound(String projectId) implements ValidationQueryResult {
    public ValidationProjectNotFound {
        Objects.requireNonNull(projectId, "projectId");
        if (projectId.isBlank()) throw new IllegalArgumentException("projectId must not be blank");
    }
}
