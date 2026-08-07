package ru.kuznetsov.qaip.explorer.application;

public record OperationOverviewProjectionProjectNotFound(
        String repositoryId
) implements OperationOverviewProjectionResult {
    public OperationOverviewProjectionProjectNotFound {
        repositoryId = OperationOverviewProjectionResult.requireId(repositoryId, "repositoryId");
    }
}
