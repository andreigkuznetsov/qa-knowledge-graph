package ru.kuznetsov.qaip.core.application.query.operationlist;

import java.util.Objects;

/**
 * Runtime-owned operation data. Test and check counts are qualified verification inputs;
 * Runtime does not currently own semantics for a final operation verification status.
 */
public record OperationQueryResult(
        String operationId,
        String method,
        String path,
        String displayName,
        int testCount,
        int checkCount
) {
    public OperationQueryResult {
        operationId = requireText(operationId, "operationId");
        method = requireText(method, "method");
        path = requireText(path, "path");
        displayName = requireText(displayName, "displayName");
        requireNonNegative(testCount, "testCount");
        requireNonNegative(checkCount, "checkCount");
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
