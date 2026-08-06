package ru.kuznetsov.qaip.explorer.application;

public record OperationTestsProjectionNoneQualified(String repositoryId, String operationId)
        implements OperationTestsProjectionResult {
    public OperationTestsProjectionNoneQualified {
        repositoryId = OperationTestsProjectionResult.requireId(repositoryId, "repositoryId");
        operationId = OperationTestsProjectionResult.requireId(operationId, "operationId");
    }
}
