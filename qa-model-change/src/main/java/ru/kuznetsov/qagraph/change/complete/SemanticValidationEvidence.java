package ru.kuznetsov.qagraph.change.complete;

import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;
import ru.kuznetsov.qagraph.validationcore.model.ValidationSeverity;
import java.util.List;
import java.util.Objects;

/** Read-only authoritative semantic evidence created by the complete validator. */
public final class SemanticValidationEvidence {
    private final List<ValidationIssue> authoritativeIssues;
    private final List<CompleteValidationDiagnostic> diagnostics;
    SemanticValidationEvidence(List<ValidationIssue> issues, List<CompleteValidationDiagnostic> diagnostics) {
        this.authoritativeIssues = immutable(issues, "authoritativeIssues");
        this.diagnostics = immutable(diagnostics, "diagnostics");
        if (this.diagnostics.stream().anyMatch(value -> value.origin() != CompleteValidationDiagnosticOrigin.SEMANTIC)) throw new IllegalArgumentException("semantic diagnostics must have SEMANTIC origin");
    }
    public List<ValidationIssue> authoritativeIssues() { return authoritativeIssues; }
    public List<CompleteValidationDiagnostic> diagnostics() { return diagnostics; }
    public boolean valid() { return diagnostics.stream().noneMatch(value -> value.severity() == ValidationSeverity.ERROR); }
    private static <T> List<T> immutable(List<T> values, String name) { Objects.requireNonNull(values, name); if (values.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException(name + " contains null"); return List.copyOf(values); }
    @Override public boolean equals(Object o) { return o instanceof SemanticValidationEvidence that && authoritativeIssues.equals(that.authoritativeIssues) && diagnostics.equals(that.diagnostics); }
    @Override public int hashCode() { return Objects.hash(authoritativeIssues, diagnostics); }
}
