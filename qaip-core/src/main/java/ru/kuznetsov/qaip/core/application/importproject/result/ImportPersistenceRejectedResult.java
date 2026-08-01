package ru.kuznetsov.qaip.core.application.importproject.result;

import java.util.Objects;

public record ImportPersistenceRejectedResult(
        String code,
        String message
) implements ImportResult {
    public ImportPersistenceRejectedResult {
        code = requireNonBlank(code, "code");
        message = requireNonBlank(message, "message");
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
