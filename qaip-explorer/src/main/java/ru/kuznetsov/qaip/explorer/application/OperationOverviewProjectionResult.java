package ru.kuznetsov.qaip.explorer.application;

import java.util.Objects;

public sealed interface OperationOverviewProjectionResult permits OperationOverviewProjectionFound,
        OperationOverviewProjectionProjectNotFound, OperationOverviewProjectionOperationNotFound {
    static String requireId(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }
}
