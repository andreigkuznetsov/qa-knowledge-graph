package ru.kuznetsov.qaip.core.application.query.validation;

import ru.kuznetsov.qaip.core.application.validation.ValidationReport;

import java.util.Objects;

public final class ValidationReportMapper {
    public ValidationReportResult map(ValidationReport report) {
        Objects.requireNonNull(report, "report");
        return new ValidationReportResult(
                report.isValid(),
                report.errorCount(),
                report.warningCount(),
                report.issues().stream()
                        .map(issue -> new ValidationIssueResult(
                                issue.ruleId(),
                                issue.code(),
                                issue.severity().name(),
                                issue.message(),
                                issue.nodeId(),
                                issue.relationshipId()))
                        .toList());
    }
}
