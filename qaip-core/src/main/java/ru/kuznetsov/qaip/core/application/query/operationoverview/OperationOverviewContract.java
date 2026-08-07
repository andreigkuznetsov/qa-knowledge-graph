package ru.kuznetsov.qaip.core.application.query.operationoverview;

import java.util.Objects;

final class OperationOverviewContract {
    private OperationOverviewContract() { }

    static String requireId(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }
}
