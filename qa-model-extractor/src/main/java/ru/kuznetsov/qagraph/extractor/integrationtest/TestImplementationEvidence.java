package ru.kuznetsov.qagraph.extractor.integrationtest;

import java.util.Objects;

public record TestImplementationEvidence(
        String testClass,
        String testMethod,
        String displayName,
        String repositoryRelativePath,
        int line,
        int column
) {
    public TestImplementationEvidence {
        requireNonBlank(testClass, "testClass");
        requireNonBlank(testMethod, "testMethod");
        requireNonBlank(repositoryRelativePath, "repositoryRelativePath");
        if (line < 1) throw new IllegalArgumentException("line must be positive");
        if (column < 1) throw new IllegalArgumentException("column must be positive");
    }

    private static void requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
    }
}
