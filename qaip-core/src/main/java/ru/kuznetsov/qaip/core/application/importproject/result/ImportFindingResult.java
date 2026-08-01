package ru.kuznetsov.qaip.core.application.importproject.result;

import java.util.Objects;

public record ImportFindingResult(
        String code,
        String severity,
        String message,
        String path
) {
    public ImportFindingResult {
        code = requireNonBlank(code, "code");
        severity = requireNonBlank(severity, "severity");
        message = requireNonBlank(message, "message");
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
