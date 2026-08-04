package ru.kuznetsov.qaip.core.application.query.operationlist;

import ru.kuznetsov.qaip.core.persistence.read.ProjectReader;

import java.util.Objects;

public final class DefaultOperationListQuery implements OperationListQuery {
    private final ProjectReader projectReader;
    private final OperationListProjector projector;

    public DefaultOperationListQuery(ProjectReader projectReader, OperationListProjector projector) {
        this.projectReader = Objects.requireNonNull(projectReader, "projectReader");
        this.projector = Objects.requireNonNull(projector, "projector");
    }

    @Override
    public OperationListQueryResult execute(String projectId) {
        Objects.requireNonNull(projectId, "projectId");
        if (projectId.isBlank()) throw new IllegalArgumentException("projectId must not be blank");
        var project = Objects.requireNonNull(projectReader.findById(projectId), "project reader result");
        if (project.isEmpty()) return new OperationListProjectNotFound(projectId);
        return new OperationListFound(
                Objects.requireNonNull(projector.project(project.orElseThrow()), "projector result"));
    }
}
