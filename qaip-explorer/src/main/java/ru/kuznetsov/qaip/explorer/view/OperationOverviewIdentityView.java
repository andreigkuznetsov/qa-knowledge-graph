package ru.kuznetsov.qaip.explorer.view;

import java.util.Objects;

public record OperationOverviewIdentityView(
        String repositoryId,
        String operationId,
        String method,
        String path,
        String displayName
) {
    public OperationOverviewIdentityView {
        repositoryId = requireText(repositoryId, "repositoryId");
        operationId = requireText(operationId, "operationId");
        method = requireText(method, "method");
        path = requireText(path, "path");
        displayName = requireText(displayName, "displayName");
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }
}
