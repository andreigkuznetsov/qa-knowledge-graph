package ru.kuznetsov.qaip.core.application.query.operationdetails;

import java.util.Objects;

public record OperationDetailsResult(
        String operationId,
        String method,
        String path,
        String displayName,
        int testCount,
        int checkCount,
        String controllerName,
        String serviceName,
        String repositoryName
) {
    public OperationDetailsResult {
        operationId = requireText(operationId, "operationId");
        method = requireText(method, "method");
        path = requireText(path, "path");
        displayName = requireText(displayName, "displayName");
        requireNonNegative(testCount, "testCount");
        requireNonNegative(checkCount, "checkCount");
        controllerName = requireText(controllerName, "controllerName");
        serviceName = requireText(serviceName, "serviceName");
        repositoryName = requireText(repositoryName, "repositoryName");
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }

    private static void requireNonNegative(int value, String field) {
        if (value < 0) throw new IllegalArgumentException(field + " must not be negative");
    }
}
