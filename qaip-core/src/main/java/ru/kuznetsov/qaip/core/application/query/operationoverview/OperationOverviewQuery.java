package ru.kuznetsov.qaip.core.application.query.operationoverview;

public interface OperationOverviewQuery {
    OperationOverviewQueryResult execute(String projectId, String operationId);
}
