package ru.kuznetsov.qaip.explorer.view;

import java.util.Objects;

public record OperationCheckView(String displayName, String checkType) {
    public OperationCheckView {
        displayName = requireText(displayName, "displayName");
        checkType = requireText(checkType, "checkType");
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }
}
