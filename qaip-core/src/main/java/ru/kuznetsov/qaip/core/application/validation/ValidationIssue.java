package ru.kuznetsov.qaip.core.application.validation;

import java.util.Objects;

public record ValidationIssue(
        String ruleId,
        String code,
        ValidationSeverity severity,
        String message,
        String nodeId,
        String relationshipId
) {
    public ValidationIssue {
        ruleId = requireNonBlank(ruleId, "ruleId");
        code = requireNonBlank(code, "code");
        Objects.requireNonNull(severity, "severity");
        message = requireNonBlank(message, "message");
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
