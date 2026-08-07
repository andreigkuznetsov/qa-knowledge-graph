package ru.kuznetsov.qaip.explorer.application;

public interface GetOperationTestsService {
    OperationTestsProjectionResult getOperationTests(String repositoryId, String operationId);
}
