package ru.kuznetsov.qaip.core.application.query.trace;

import java.util.Objects;

public record TraceNodeNotFound(
        String projectId,
        String startNodeId
) implements TraceQueryResult {
    public TraceNodeNotFound {
        projectId = requireNonBlank(projectId, "projectId");
        startNodeId = requireNonBlank(startNodeId, "startNodeId");
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
