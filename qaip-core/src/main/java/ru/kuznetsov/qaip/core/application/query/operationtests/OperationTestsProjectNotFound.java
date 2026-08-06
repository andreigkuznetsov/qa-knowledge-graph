package ru.kuznetsov.qaip.core.application.query.operationtests;

import java.util.Objects;

public record OperationTestsProjectNotFound(String projectId) implements OperationTestsQueryResult {
    public OperationTestsProjectNotFound {
        projectId = requireId(projectId, "projectId");
    }

    static String requireId(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }
}
