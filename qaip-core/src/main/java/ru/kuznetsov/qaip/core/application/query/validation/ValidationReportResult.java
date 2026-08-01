package ru.kuznetsov.qaip.core.application.query.validation;

import java.util.List;

public record ValidationReportResult(
        boolean valid,
        long errorCount,
        long warningCount,
        List<ValidationIssueResult> issues
) {
    public ValidationReportResult {
        if (errorCount < 0) throw new IllegalArgumentException("errorCount must not be negative");
        if (warningCount < 0) throw new IllegalArgumentException("warningCount must not be negative");
        if (valid != (errorCount == 0)) {
            throw new IllegalArgumentException("valid must equal whether errorCount is zero");
        }
        issues = List.copyOf(issues);
    }
}
