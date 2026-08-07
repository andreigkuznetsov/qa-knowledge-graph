package ru.kuznetsov.qaip.explorer.application;

public record OperationOverviewProjectionOperationNotFound(
        String repositoryId,
        String operationId
) implements OperationOverviewProjectionResult {
    public OperationOverviewProjectionOperationNotFound {
        repositoryId = OperationOverviewProjectionResult.requireId(repositoryId, "repositoryId");
        operationId = OperationOverviewProjectionResult.requireId(operationId, "operationId");
    }
}
