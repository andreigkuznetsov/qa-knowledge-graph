package ru.kuznetsov.qaip.core.application.persistence;

import java.util.Objects;

public record PersistProjectFinding(PersistProjectFindingCode code, String message, String projectId) {
    public PersistProjectFinding {
        Objects.requireNonNull(code, "code");
        message = requireNonBlank(message, "message");
        projectId = requireNonBlank(projectId, "projectId");
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
