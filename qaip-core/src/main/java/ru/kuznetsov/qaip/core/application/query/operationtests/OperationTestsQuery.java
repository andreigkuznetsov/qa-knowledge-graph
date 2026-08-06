package ru.kuznetsov.qaip.core.application.query.operationtests;

public interface OperationTestsQuery {
    OperationTestsQueryResult execute(String projectId, String operationId);
}
