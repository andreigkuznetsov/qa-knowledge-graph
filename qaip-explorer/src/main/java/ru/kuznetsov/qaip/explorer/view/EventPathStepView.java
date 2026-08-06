package ru.kuznetsov.qaip.explorer.view;

import java.util.Objects;

public record EventPathStepView(
        String implementationRole,
        String displayName,
        String implementationType,
        String technology
) {
    public EventPathStepView {
        requireNonBlank(implementationRole, "implementationRole");
        requireNonBlank(displayName, "displayName");
        requireNonBlank(implementationType, "implementationType");
        if (technology != null && technology.isBlank()) {
            throw new IllegalArgumentException("technology must be null or non-blank");
        }
    }

    private static void requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
    }
}
