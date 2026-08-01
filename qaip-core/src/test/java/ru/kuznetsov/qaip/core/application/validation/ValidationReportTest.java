package ru.kuznetsov.qaip.core.application.validation;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidationReportTest {
    @Test
    void accepts_empty_ordered_and_duplicate_issues_with_value_semantics() {
        assertEquals(List.of(), new ValidationReport(List.of()).issues());
        ValidationIssue warning = issue("W", ValidationSeverity.WARNING);
        ValidationIssue error = issue("E", ValidationSeverity.ERROR);
        ValidationReport report = new ValidationReport(List.of(warning, error, warning));
        assertEquals(List.of(warning, error, warning), report.issues());
        assertEquals(report, new ValidationReport(List.of(warning, error, warning)));
        assertEquals(report.hashCode(), new ValidationReport(List.of(warning, error, warning)).hashCode());
    }

    @Test
    void defensively_copies_and_exposes_an_immutable_list() {
        var source = new ArrayList<>(List.of(issue("W", ValidationSeverity.WARNING)));
        ValidationReport report = new ValidationReport(source);
        source.clear();
        assertEquals(1, report.issues().size());
        assertThrows(UnsupportedOperationException.class, () -> report.issues().clear());
        assertThrows(NullPointerException.class, () -> new ValidationReport(null));
        var withNull = new ArrayList<ValidationIssue>();
        withNull.add(null);
        assertThrows(NullPointerException.class, () -> new ValidationReport(withNull));
    }

    @Test
    void validity_and_counts_are_pure_severity_queries() {
        ValidationReport empty = new ValidationReport(List.of());
        ValidationReport warnings = new ValidationReport(List.of(
                issue("W1", ValidationSeverity.WARNING), issue("W2", ValidationSeverity.WARNING)));
        ValidationReport errors = new ValidationReport(List.of(
                issue("E1", ValidationSeverity.ERROR), issue("W", ValidationSeverity.WARNING),
                issue("E2", ValidationSeverity.ERROR)));
        assertTrue(empty.isValid());
        assertTrue(warnings.isValid());
        assertFalse(errors.isValid());
        assertEquals(0, warnings.errorCount());
        assertEquals(2, warnings.warningCount());
        assertEquals(2, errors.errorCount());
        assertEquals(1, errors.warningCount());
    }

    private static ValidationIssue issue(String code, ValidationSeverity severity) {
        return new ValidationIssue("RULE", code, severity, "message", null, null);
    }
}
