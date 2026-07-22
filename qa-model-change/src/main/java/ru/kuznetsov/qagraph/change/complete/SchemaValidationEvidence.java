package ru.kuznetsov.qagraph.change.complete;

import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;
import java.util.List;
import java.util.Objects;

/** Read-only authoritative schema evidence created by the complete validator. */
public final class SchemaValidationEvidence {
    private final List<ValidationIssue> authoritativeIssues;
    private final List<CompleteValidationDiagnostic> diagnostics;
    SchemaValidationEvidence(List<ValidationIssue> issues, List<CompleteValidationDiagnostic> diagnostics) {
        this.authoritativeIssues = immutable(issues, "authoritativeIssues");
        this.diagnostics = immutable(diagnostics, "diagnostics");
        if (this.diagnostics.stream().anyMatch(value -> value.origin() != CompleteValidationDiagnosticOrigin.SCHEMA)) throw new IllegalArgumentException("schema diagnostics must have SCHEMA origin");
    }
    public List<ValidationIssue> authoritativeIssues() { return authoritativeIssues; }
    public List<CompleteValidationDiagnostic> diagnostics() { return diagnostics; }
    public boolean valid() { return diagnostics.isEmpty(); }
    private static <T> List<T> immutable(List<T> values, String name) { Objects.requireNonNull(values, name); if (values.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException(name + " contains null"); return List.copyOf(values); }
    @Override public boolean equals(Object o) { return o instanceof SchemaValidationEvidence that && authoritativeIssues.equals(that.authoritativeIssues) && diagnostics.equals(that.diagnostics); }
    @Override public int hashCode() { return Objects.hash(authoritativeIssues, diagnostics); }
}
