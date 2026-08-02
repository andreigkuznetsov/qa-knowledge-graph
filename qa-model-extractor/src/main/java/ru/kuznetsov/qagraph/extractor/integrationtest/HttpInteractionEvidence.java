package ru.kuznetsov.qagraph.extractor.integrationtest;

import java.util.Objects;

public record HttpInteractionEvidence(
        IntegrationHttpMethod httpMethod,
        String endpointPath,
        String sourceExpression,
        String owningTestClass,
        String owningTestMethod,
        String repositoryRelativePath,
        int line,
        int column,
        String invocationDetails
) {
    public HttpInteractionEvidence {
        Objects.requireNonNull(httpMethod, "httpMethod");
        requireNonBlank(endpointPath, "endpointPath");
        requireNonBlank(sourceExpression, "sourceExpression");
        requireNonBlank(owningTestClass, "owningTestClass");
        requireNonBlank(owningTestMethod, "owningTestMethod");
        requireNonBlank(repositoryRelativePath, "repositoryRelativePath");
        if (line < 1) throw new IllegalArgumentException("line must be positive");
        if (column < 1) throw new IllegalArgumentException("column must be positive");
    }

    public HttpInteractionEvidence(
            IntegrationHttpMethod httpMethod,
            String endpointPath,
            String sourceExpression,
            String owningTestClass,
            String owningTestMethod,
            String repositoryRelativePath,
            int line,
            int column) {
        this(httpMethod, endpointPath, sourceExpression, owningTestClass, owningTestMethod,
                repositoryRelativePath, line, column, null);
    }

    private static void requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
    }
}
