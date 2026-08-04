package ru.kuznetsov.qaip.core.application.query.operationdetails;

import java.util.Objects;

public record OperationDetailsUnavailable(
        String projectId,
        String operationId,
        OperationDetailsUnavailableReason reason
) implements OperationDetailsQueryResult {
    public OperationDetailsUnavailable {
        projectId = OperationDetailsProjectNotFound.requireId(projectId, "projectId");
        operationId = OperationDetailsProjectNotFound.requireId(operationId, "operationId");
        Objects.requireNonNull(reason, "reason");
    }
}
