package ru.kuznetsov.qaip.core.application.query.eventpath;

import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.persistence.read.ProjectReader;

import java.util.Objects;

public final class DefaultEventPathQuery implements EventPathQuery {
    private static final String BUSINESS_OPERATION = "BUSINESS_OPERATION";

    private final ProjectReader projectReader;
    private final EventPathResolver resolver;

    public DefaultEventPathQuery(ProjectReader projectReader) {
        this(projectReader, new EventPathResolver());
    }

    public DefaultEventPathQuery(ProjectReader projectReader, EventPathResolver resolver) {
        this.projectReader = Objects.requireNonNull(projectReader, "projectReader");
        this.resolver = Objects.requireNonNull(resolver, "resolver");
    }

    @Override
    public EventPathQueryResult execute(String projectId, String operationId) {
        String requestedProjectId = EventPathProjectNotFound.requireId(projectId, "projectId");
        String requestedOperationId = EventPathProjectNotFound.requireId(operationId, "operationId");
        var project = Objects.requireNonNull(
                projectReader.findById(requestedProjectId), "project reader result");
        if (project.isEmpty()) return new EventPathProjectNotFound(requestedProjectId);
        Project value = project.orElseThrow();
        if (value.nodes().stream().noneMatch(node ->
                requestedOperationId.equals(node.id()) && BUSINESS_OPERATION.equals(node.type()))) {
            return new EventPathOperationNotFound(requestedProjectId, requestedOperationId);
        }
        return resolver.resolve(value, requestedProjectId, requestedOperationId);
    }
}
