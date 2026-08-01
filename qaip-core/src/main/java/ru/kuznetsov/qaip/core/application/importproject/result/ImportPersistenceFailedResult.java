package ru.kuznetsov.qaip.core.application.importproject.result;

import java.util.Objects;

public record ImportPersistenceFailedResult(String message) implements ImportResult {
    public ImportPersistenceFailedResult {
        Objects.requireNonNull(message, "message");
        if (message.isBlank()) throw new IllegalArgumentException("message must not be blank");
    }
}
