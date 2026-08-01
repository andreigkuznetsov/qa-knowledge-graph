package ru.kuznetsov.qaip.core.application.importproject;

public record ImportProjectPersistenceFailed(String message) implements ImportProjectUseCaseResult {
    public ImportProjectPersistenceFailed {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }
}
