package ru.kuznetsov.qaip.core.application.query.operationoverview;

public record OperationOverviewProjectNotFound(String projectId) implements OperationOverviewQueryResult {
    public OperationOverviewProjectNotFound {
        projectId = OperationOverviewContract.requireId(projectId, "projectId");
    }
}
