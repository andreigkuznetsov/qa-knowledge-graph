package ru.kuznetsov.qaip.core.application.query.operationoverview;

public record OperationOverviewOperationNotFound(
        String projectId,
        String operationId
) implements OperationOverviewQueryResult {
    public OperationOverviewOperationNotFound {
        projectId = OperationOverviewContract.requireId(projectId, "projectId");
        operationId = OperationOverviewContract.requireId(operationId, "operationId");
    }
}
