package ru.kuznetsov.qaip.explorer.view;

import java.util.List;
import java.util.Objects;

public record OperationTestView(
        String displayName,
        String testClass,
        String testMethod,
        int checkCount,
        List<OperationCheckView> checks
) {
    public OperationTestView {
        displayName = requireText(displayName, "displayName");
        testClass = requireText(testClass, "testClass");
        testMethod = requireText(testMethod, "testMethod");
        checks = List.copyOf(Objects.requireNonNull(checks, "checks"));
        if (checkCount != checks.size()) throw new IllegalArgumentException("checkCount must equal checks.size");
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }
}
