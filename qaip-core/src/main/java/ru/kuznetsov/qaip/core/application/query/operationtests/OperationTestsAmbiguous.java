package ru.kuznetsov.qaip.core.application.query.operationtests;

public record OperationTestsAmbiguous(
        String projectId,
        String operationId
) implements OperationTestsQueryResult {
    public OperationTestsAmbiguous {
        projectId = OperationTestsProjectNotFound.requireId(projectId, "projectId");
        operationId = OperationTestsProjectNotFound.requireId(operationId, "operationId");
    }
}
