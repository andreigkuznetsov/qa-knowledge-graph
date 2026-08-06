package ru.kuznetsov.qaip.explorer.view;

import java.util.List;
import java.util.Objects;

public record EventPathView(
        String projectId,
        String operationId,
        String pathKind,
        List<EventPathStepView> steps
) {
    public EventPathView {
        requireNonBlank(projectId, "projectId");
        requireNonBlank(operationId, "operationId");
        requireNonBlank(pathKind, "pathKind");
        steps = List.copyOf(Objects.requireNonNull(steps, "steps"));
    }

    private static void requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
    }
}
