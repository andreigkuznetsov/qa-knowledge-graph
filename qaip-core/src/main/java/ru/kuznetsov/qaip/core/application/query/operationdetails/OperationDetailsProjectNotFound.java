package ru.kuznetsov.qaip.core.application.query.operationdetails;

import java.util.Objects;

public record OperationDetailsProjectNotFound(String projectId) implements OperationDetailsQueryResult {
    public OperationDetailsProjectNotFound {
        projectId = requireId(projectId, "projectId");
    }

    static String requireId(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }
}
