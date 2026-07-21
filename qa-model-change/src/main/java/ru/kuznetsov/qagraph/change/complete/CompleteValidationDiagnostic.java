package ru.kuznetsov.qagraph.change.complete;

import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;
import ru.kuznetsov.qagraph.validationcore.model.ValidationSeverity;

import java.util.Comparator;
import java.util.Objects;

/** Stable normalized view retaining the authoritative issue. */
public record CompleteValidationDiagnostic(
        CompleteValidationDiagnosticOrigin origin,
        ValidationSeverity severity,
        String code,
        String path,
        String message,
        String objectId,
        ValidationIssue authoritativeIssue
) {
    public static final Comparator<CompleteValidationDiagnostic> ORDER =
            Comparator
                    .comparingInt((CompleteValidationDiagnostic value) ->
                            value.origin().rank())
                    .thenComparingInt(value -> severityRank(value.severity()))
                    .thenComparing(CompleteValidationDiagnostic::path)
                    .thenComparing(CompleteValidationDiagnostic::code)
                    .thenComparing(CompleteValidationDiagnostic::message)
                    .thenComparing(value -> value.objectId() == null
                            ? "" : value.objectId());

    public CompleteValidationDiagnostic {
        Objects.requireNonNull(origin, "origin must not be null");
        Objects.requireNonNull(severity, "severity must not be null");
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(
                authoritativeIssue,
                "authoritativeIssue must not be null"
        );
        if (code.isBlank() || message.isBlank()) {
            throw new IllegalArgumentException(
                    "code and message must not be blank"
            );
        }
    }

    private static int severityRank(ValidationSeverity severity) {
        return switch (severity) {
            case ERROR -> 0;
            case WARNING -> 1;
        };
    }
}
