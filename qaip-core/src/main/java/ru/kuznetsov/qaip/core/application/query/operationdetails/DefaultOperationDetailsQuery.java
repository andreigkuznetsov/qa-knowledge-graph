package ru.kuznetsov.qaip.core.application.query.operationdetails;

import ru.kuznetsov.qaip.core.application.query.operationlist.OperationListProjector;
import ru.kuznetsov.qaip.core.application.query.operationlist.OperationQueryResult;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.persistence.read.ProjectReader;

import java.util.Objects;

public final class DefaultOperationDetailsQuery implements OperationDetailsQuery {
    private static final String BUSINESS_OPERATION = "BUSINESS_OPERATION";
    private final ProjectReader projectReader;
    private final OperationListProjector operationProjector;
    private final ConventionalImplementationPathResolver pathResolver;

    public DefaultOperationDetailsQuery(ProjectReader projectReader, OperationListProjector operationProjector) {
        this(projectReader, operationProjector, new ConventionalImplementationPathResolver());
    }

    public DefaultOperationDetailsQuery(
            ProjectReader projectReader,
            OperationListProjector operationProjector,
            ConventionalImplementationPathResolver pathResolver
    ) {
        this.projectReader = Objects.requireNonNull(projectReader, "projectReader");
        this.operationProjector = Objects.requireNonNull(operationProjector, "operationProjector");
        this.pathResolver = Objects.requireNonNull(pathResolver, "pathResolver");
    }

    @Override
    public OperationDetailsQueryResult execute(String projectId, String operationId) {
        OperationDetailsProjectNotFound.requireId(projectId, "projectId");
        OperationDetailsProjectNotFound.requireId(operationId, "operationId");
        var project = Objects.requireNonNull(projectReader.findById(projectId), "project reader result");
        if (project.isEmpty()) return new OperationDetailsProjectNotFound(projectId);
        Project value = project.orElseThrow();
        if (value.nodes().stream().noneMatch(node ->
                operationId.equals(node.id()) && BUSINESS_OPERATION.equals(node.type()))) {
            return new OperationDetailsOperationNotFound(projectId, operationId);
        }

        OperationQueryResult operation = operationProjector.project(value).stream()
                .filter(candidate -> operationId.equals(candidate.operationId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Operation projector omitted an existing operation"));
        var path = pathResolver.resolve(value, operationId);
        if (path instanceof ConventionalImplementationPathResolver.Unavailable unavailable) {
            return new OperationDetailsUnavailable(projectId, operationId, unavailable.reason());
        }
        var available = (ConventionalImplementationPathResolver.Available) path;
        return new OperationDetailsFound(new OperationDetailsResult(
                operation.operationId(), operation.method(), operation.path(), operation.displayName(),
                operation.testCount(), operation.checkCount(), available.controller().name(),
                available.service().name(), available.repository().name()));
    }
}
