package ru.kuznetsov.qaip.core.application.persistence;

import java.util.Objects;

public record PersistProjectAccepted(String projectId) implements PersistProjectResult {
    public PersistProjectAccepted {
        Objects.requireNonNull(projectId, "projectId");
        if (projectId.isBlank()) throw new IllegalArgumentException("projectId must not be blank");
    }
}
