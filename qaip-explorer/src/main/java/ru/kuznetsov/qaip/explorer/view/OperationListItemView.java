package ru.kuznetsov.qaip.explorer.view;

import java.util.Objects;

public record OperationListItemView(
        String operationId,
        String method,
        String path,
        String displayName,
        OperationVerificationStatus verificationStatus,
        int testCount,
        int checkCount
) {
    public OperationListItemView {
        Objects.requireNonNull(operationId, "operationId");
        requireNonBlank(method, "method");
        requireNonBlank(path, "path");
        requireNonBlank(displayName, "displayName");
        Objects.requireNonNull(verificationStatus, "verificationStatus");
        requireNonNegative(testCount, "testCount");
        requireNonNegative(checkCount, "checkCount");
    }

    private static void requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
    }

    private static void requireNonNegative(int value, String field) {
        if (value < 0) throw new IllegalArgumentException(field + " must not be negative");
    }
}
