package ru.kuznetsov.qaip.core.application.query.operationlist;

import java.util.Objects;

public record OperationListProjectNotFound(String projectId) implements OperationListQueryResult {
    public OperationListProjectNotFound {
        Objects.requireNonNull(projectId, "projectId");
        if (projectId.isBlank()) throw new IllegalArgumentException("projectId must not be blank");
    }
}
