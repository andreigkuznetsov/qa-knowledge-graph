package ru.kuznetsov.qaip.core.application.query.trace;

import java.util.Objects;

public record TraceProjectNotFound(String projectId) implements TraceQueryResult {
    public TraceProjectNotFound {
        Objects.requireNonNull(projectId, "projectId");
        if (projectId.isBlank()) throw new IllegalArgumentException("projectId must not be blank");
    }
}
