package ru.kuznetsov.qaip.core.application.query.operationtests;

public record OperationTestsOperationNotFound(
        String projectId,
        String operationId
) implements OperationTestsQueryResult {
    public OperationTestsOperationNotFound {
        projectId = OperationTestsProjectNotFound.requireId(projectId, "projectId");
        operationId = OperationTestsProjectNotFound.requireId(operationId, "operationId");
    }
}
