package ru.kuznetsov.qaip.core.application.query.operationlist;

public interface OperationListQuery {
    OperationListQueryResult execute(String projectId);
}
