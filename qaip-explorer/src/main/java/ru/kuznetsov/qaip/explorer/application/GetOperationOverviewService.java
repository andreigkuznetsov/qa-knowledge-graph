package ru.kuznetsov.qaip.explorer.application;

public interface GetOperationOverviewService {
    OperationOverviewProjectionResult getOperationOverview(String repositoryId, String operationId);
}
