package ru.kuznetsov.qaip.core.application.query.validation;

import java.util.Objects;

public record ValidationCompleted(
        String projectId,
        ValidationReportResult report
) implements ValidationQueryResult {
    public ValidationCompleted {
        Objects.requireNonNull(projectId, "projectId");
        if (projectId.isBlank()) throw new IllegalArgumentException("projectId must not be blank");
        Objects.requireNonNull(report, "report");
    }
}
