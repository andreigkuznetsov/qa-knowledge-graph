package ru.kuznetsov.qaip.explorer.application;

public record OperationTestsProjectionAmbiguous(String repositoryId, String operationId)
        implements OperationTestsProjectionResult {
    public OperationTestsProjectionAmbiguous {
        repositoryId = OperationTestsProjectionResult.requireId(repositoryId, "repositoryId");
        operationId = OperationTestsProjectionResult.requireId(operationId, "operationId");
    }
}
