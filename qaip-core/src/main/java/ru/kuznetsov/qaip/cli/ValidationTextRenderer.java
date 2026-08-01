package ru.kuznetsov.qaip.cli;

import ru.kuznetsov.qaip.core.application.query.validation.ValidationIssueResult;
import ru.kuznetsov.qaip.core.application.query.validation.ValidationReportResult;

import java.util.List;
import java.util.Objects;

final class ValidationTextRenderer {
    String renderCompleted(String projectId, ValidationReportResult report) {
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(report, "report");
        return String.join(System.lineSeparator(),
                "Validation Report",
                "Project ID: " + projectId,
                "Status: " + (report.valid() ? "VALID" : "INVALID"),
                "Errors: " + report.errorCount(),
                "Warnings: " + report.warningCount(),
                "",
                "Issues:",
                renderIssues(report.issues()));
    }

    private static String renderIssues(List<ValidationIssueResult> issues) {
        if (issues.isEmpty()) return "(none)";
        return String.join(System.lineSeparator(), issues.stream()
                .map(ValidationTextRenderer::renderIssue)
                .toList());
    }

    private static String renderIssue(ValidationIssueResult issue) {
        String line = "[" + issue.severity() + "] " + issue.code() + " | rule=" + issue.ruleId()
                + " | " + issue.message();
        if (issue.nodeId() != null) line += " | node=" + issue.nodeId();
        if (issue.relationshipId() != null) line += " | relationship=" + issue.relationshipId();
        return line;
    }
}
