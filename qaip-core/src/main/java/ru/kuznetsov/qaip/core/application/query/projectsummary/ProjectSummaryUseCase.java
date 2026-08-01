package ru.kuznetsov.qaip.core.application.query.projectsummary;

public interface ProjectSummaryUseCase {
    ProjectSummaryQueryResult execute(String projectId);
}
