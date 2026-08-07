package ru.kuznetsov.qaip.core.application.query.operationoverview;

public record OperationOverviewIdentity(
        String projectId,
        String operationId,
        String method,
        String path,
        String displayName
) {
    public OperationOverviewIdentity {
        projectId = OperationOverviewContract.requireId(projectId, "projectId");
        operationId = OperationOverviewContract.requireId(operationId, "operationId");
        method = OperationOverviewContract.requireId(method, "method");
        path = OperationOverviewContract.requireId(path, "path");
        displayName = OperationOverviewContract.requireId(displayName, "displayName");
    }
}
