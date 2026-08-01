package ru.kuznetsov.qaip.core.application.validation;

import java.util.List;

public record ValidationReport(List<ValidationIssue> issues) {
    public ValidationReport {
        issues = List.copyOf(issues);
    }

    public boolean isValid() {
        return issues.stream().noneMatch(issue -> issue.severity() == ValidationSeverity.ERROR);
    }

    public long errorCount() {
        return count(ValidationSeverity.ERROR);
    }

    public long warningCount() {
        return count(ValidationSeverity.WARNING);
    }

    private long count(ValidationSeverity severity) {
        return issues.stream().filter(issue -> issue.severity() == severity).count();
    }
}
