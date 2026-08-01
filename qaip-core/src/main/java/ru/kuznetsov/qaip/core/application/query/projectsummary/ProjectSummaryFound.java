package ru.kuznetsov.qaip.core.application.query.projectsummary;

import java.util.Objects;

public record ProjectSummaryFound(ProjectSummaryResult summary) implements ProjectSummaryQueryResult {
    public ProjectSummaryFound {
        Objects.requireNonNull(summary, "summary");
    }
}
