package ru.kuznetsov.qaip.core.application.query.trace;

import java.util.Objects;

public record TraceFound(
        String projectId,
        String startNodeId,
        TraceResult trace
) implements TraceQueryResult {
    public TraceFound {
        projectId = requireNonBlank(projectId, "projectId");
        startNodeId = requireNonBlank(startNodeId, "startNodeId");
        Objects.requireNonNull(trace, "trace");
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
