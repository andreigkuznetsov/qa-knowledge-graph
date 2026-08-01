package ru.kuznetsov.qaip.core.application.query.nodedetails;

import java.util.Objects;

public record NodeDetailsProjectNotFound(String projectId) implements NodeDetailsQueryResult {
    public NodeDetailsProjectNotFound {
        projectId = requireId(projectId, "projectId");
    }

    static String requireId(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
