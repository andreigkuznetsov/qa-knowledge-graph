package ru.kuznetsov.qaip.core.application.query.projectsummary;

import ru.kuznetsov.qaip.core.persistence.read.ProjectReader;

import java.util.Objects;

public final class DefaultProjectSummaryUseCase implements ProjectSummaryUseCase {
    private final ProjectReader projectReader;
    private final ProjectSummaryMapper summaryMapper;

    public DefaultProjectSummaryUseCase(ProjectReader projectReader, ProjectSummaryMapper summaryMapper) {
        this.projectReader = Objects.requireNonNull(projectReader, "projectReader");
        this.summaryMapper = Objects.requireNonNull(summaryMapper, "summaryMapper");
    }

    @Override
    public ProjectSummaryQueryResult execute(String projectId) {
        Objects.requireNonNull(projectId, "projectId");
        if (projectId.isBlank()) throw new IllegalArgumentException("projectId must not be blank");
        var project = Objects.requireNonNull(projectReader.findById(projectId), "project reader result");
        if (project.isEmpty()) return new ProjectSummaryNotFound(projectId);
        var summary = Objects.requireNonNull(summaryMapper.map(project.orElseThrow()), "summary mapper result");
        return new ProjectSummaryFound(summary);
    }
}
