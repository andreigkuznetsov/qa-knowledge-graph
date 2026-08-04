package ru.kuznetsov.qaip.core.application.query.operationdetails;

public interface OperationDetailsQuery {
    OperationDetailsQueryResult execute(String projectId, String operationId);
}
