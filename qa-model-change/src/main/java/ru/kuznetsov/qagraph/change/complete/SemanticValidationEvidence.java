package ru.kuznetsov.qagraph.change.complete;

import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;
import ru.kuznetsov.qagraph.validationcore.model.ValidationSeverity;

import java.util.List;
import java.util.Objects;

public record SemanticValidationEvidence(
        List<ValidationIssue> authoritativeIssues,
        List<CompleteValidationDiagnostic> diagnostics
) {
    public SemanticValidationEvidence {
        authoritativeIssues = immutable(authoritativeIssues, "authoritativeIssues");
        diagnostics = immutable(diagnostics, "diagnostics");
        if (diagnostics.stream().anyMatch(value ->
                value.origin() != CompleteValidationDiagnosticOrigin.SEMANTIC)) {
            throw new IllegalArgumentException(
                    "semantic diagnostics must have SEMANTIC origin"
            );
        }
    }

    public boolean valid() {
        return diagnostics.stream().noneMatch(value ->
                value.severity() == ValidationSeverity.ERROR);
    }

    private static <T> List<T> immutable(List<T> values, String name) {
        Objects.requireNonNull(values, name + " must not be null");
        if (values.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(name + " contains null");
        }
        return List.copyOf(values);
    }
}
