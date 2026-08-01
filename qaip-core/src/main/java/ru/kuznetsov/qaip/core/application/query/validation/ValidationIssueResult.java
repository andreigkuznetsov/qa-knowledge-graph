package ru.kuznetsov.qaip.core.application.query.validation;

import java.util.Objects;

public record ValidationIssueResult(
        String ruleId,
        String code,
        String severity,
        String message,
        String nodeId,
        String relationshipId
) {
    public ValidationIssueResult {
        ruleId = requireNonBlank(ruleId, "ruleId");
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
