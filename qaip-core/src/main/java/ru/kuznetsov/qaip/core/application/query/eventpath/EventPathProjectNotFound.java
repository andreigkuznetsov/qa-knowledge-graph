package ru.kuznetsov.qaip.core.application.query.eventpath;

import java.util.Objects;

public record EventPathProjectNotFound(String projectId) implements EventPathQueryResult {
    public EventPathProjectNotFound {
        projectId = requireId(projectId, "projectId");
    }

    static String requireId(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }
}
