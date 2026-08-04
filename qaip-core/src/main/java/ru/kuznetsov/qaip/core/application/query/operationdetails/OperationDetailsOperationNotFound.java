package ru.kuznetsov.qaip.core.application.query.operationdetails;

public record OperationDetailsOperationNotFound(
        String projectId,
        String operationId
) implements OperationDetailsQueryResult {
    public OperationDetailsOperationNotFound {
        projectId = OperationDetailsProjectNotFound.requireId(projectId, "projectId");
        operationId = OperationDetailsProjectNotFound.requireId(operationId, "operationId");
    }
}
