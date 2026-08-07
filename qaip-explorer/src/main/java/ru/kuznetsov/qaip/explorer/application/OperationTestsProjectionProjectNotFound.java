package ru.kuznetsov.qaip.explorer.application;

public record OperationTestsProjectionProjectNotFound(String repositoryId)
        implements OperationTestsProjectionResult {
    public OperationTestsProjectionProjectNotFound {
        repositoryId = OperationTestsProjectionResult.requireId(repositoryId, "repositoryId");
    }
}
