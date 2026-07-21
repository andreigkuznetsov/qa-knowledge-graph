package ru.kuznetsov.qagraph.change.complete;

import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;

import java.util.List;
import java.util.Objects;

public record SchemaValidationEvidence(
        List<ValidationIssue> authoritativeIssues,
        List<CompleteValidationDiagnostic> diagnostics
) {
    public SchemaValidationEvidence {
        authoritativeIssues = immutable(authoritativeIssues, "authoritativeIssues");
        diagnostics = immutable(diagnostics, "diagnostics");
        if (diagnostics.stream().anyMatch(value ->
                value.origin() != CompleteValidationDiagnosticOrigin.SCHEMA)) {
            throw new IllegalArgumentException(
                    "schema diagnostics must have SCHEMA origin"
            );
        }
    }

    public boolean valid() {
        return diagnostics.isEmpty();
    }

    private static <T> List<T> immutable(List<T> values, String name) {
        Objects.requireNonNull(values, name + " must not be null");
        if (values.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(name + " contains null");
        }
        return List.copyOf(values);
    }
}
