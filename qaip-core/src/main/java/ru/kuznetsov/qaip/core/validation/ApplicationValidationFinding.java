package ru.kuznetsov.qaip.core.validation;

import java.util.Objects;

public record ApplicationValidationFinding(String code, ApplicationValidationSeverity severity,
                                           String message, String location) {
    public ApplicationValidationFinding {
        code = requireNonBlank(code, "code");
        Objects.requireNonNull(severity, "severity");
        message = requireNonBlank(message, "message");
        location = requireNonBlank(location, "location");
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
