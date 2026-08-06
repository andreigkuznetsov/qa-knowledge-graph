package ru.kuznetsov.qaip.explorer.application;

public record OperationTestsProjectionOperationNotFound(String repositoryId, String operationId)
        implements OperationTestsProjectionResult {
    public OperationTestsProjectionOperationNotFound {
        repositoryId = OperationTestsProjectionResult.requireId(repositoryId, "repositoryId");
        operationId = OperationTestsProjectionResult.requireId(operationId, "operationId");
    }
}
