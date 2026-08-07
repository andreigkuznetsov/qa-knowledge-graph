package ru.kuznetsov.qaip.core.application.query.operationtests;

import ru.kuznetsov.qaip.core.application.query.operationlist.OperationListProjector;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.persistence.read.ProjectReader;

import java.util.Objects;

public final class DefaultOperationTestsQuery implements OperationTestsQuery {
    private static final String BUSINESS_OPERATION = "BUSINESS_OPERATION";

    private final ProjectReader projectReader;
    private final OperationTestsResolver resolver;

    public DefaultOperationTestsQuery(ProjectReader projectReader, OperationListProjector qualificationProjector) {
        this(projectReader, new DefaultOperationTestsResolver(qualificationProjector));
    }

    public DefaultOperationTestsQuery(ProjectReader projectReader, OperationTestsResolver resolver) {
        this.projectReader = Objects.requireNonNull(projectReader, "projectReader");
        this.resolver = Objects.requireNonNull(resolver, "resolver");
    }

    @Override
    public OperationTestsQueryResult execute(String projectId, String operationId) {
        String requestedProjectId = OperationTestsProjectNotFound.requireId(projectId, "projectId");
        String requestedOperationId = OperationTestsProjectNotFound.requireId(operationId, "operationId");
        var project = Objects.requireNonNull(
                projectReader.findById(requestedProjectId), "project reader result");
        if (project.isEmpty()) return new OperationTestsProjectNotFound(requestedProjectId);
        Project value = project.orElseThrow();
        if (value.nodes().stream().noneMatch(node -> requestedOperationId.equals(node.id())
                && BUSINESS_OPERATION.equals(node.type()))) {
            return new OperationTestsOperationNotFound(requestedProjectId, requestedOperationId);
        }
        return resolver.resolve(value, requestedProjectId, requestedOperationId);
    }
}
