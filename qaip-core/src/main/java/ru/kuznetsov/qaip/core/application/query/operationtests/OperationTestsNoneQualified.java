package ru.kuznetsov.qaip.core.application.query.operationtests;

public record OperationTestsNoneQualified(
        String projectId,
        String operationId
) implements OperationTestsQueryResult {
    public OperationTestsNoneQualified {
        projectId = OperationTestsProjectNotFound.requireId(projectId, "projectId");
        operationId = OperationTestsProjectNotFound.requireId(operationId, "operationId");
    }
}
