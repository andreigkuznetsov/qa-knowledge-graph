package ru.kuznetsov.qagraph.extractor.integrationtest;

import java.util.Objects;

public record AssertionEvidence(
        AssertionCategory category,
        AssertionLibrary assertionLibrary,
        String assertionKind,
        String expression,
        String owningTestClass,
        String owningTestMethod,
        String repositoryRelativePath,
        int line,
        int column,
        HelperInvocationEvidence helperInvocation
) {
    public AssertionEvidence {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(assertionLibrary, "assertionLibrary");
        requireNonBlank(assertionKind, "assertionKind");
        requireNonBlank(expression, "expression");
        requireNonBlank(owningTestClass, "owningTestClass");
        requireNonBlank(owningTestMethod, "owningTestMethod");
        requireNonBlank(repositoryRelativePath, "repositoryRelativePath");
        if (line < 1) throw new IllegalArgumentException("line must be positive");
        if (column < 1) throw new IllegalArgumentException("column must be positive");
    }

    public AssertionEvidence(
            AssertionCategory category,
            String expression,
            String owningTestClass,
            String owningTestMethod,
            String repositoryRelativePath,
            int line,
            int column) {
        this(category, AssertionLibrary.LEGACY, category.name(), expression, owningTestClass, owningTestMethod,
                repositoryRelativePath, line, column, null);
    }

    public AssertionEvidence(
            AssertionCategory category,
            String expression,
            String owningTestClass,
            String owningTestMethod,
            String repositoryRelativePath,
            int line,
            int column,
            HelperInvocationEvidence helperInvocation) {
        this(category, AssertionLibrary.LEGACY, category.name(), expression, owningTestClass,
                owningTestMethod, repositoryRelativePath, line, column, helperInvocation);
    }

    public enum AssertionLibrary {
        REST_ASSURED,
        MOCK_MVC,
        JUNIT_5,
        ASSERTJ,
        HAMCREST,
        LEGACY
    }

    public record HelperInvocationEvidence(
            String helperClass,
            String helperMethod,
            String invocationExpression,
            String repositoryRelativePath,
            int line,
            int column
    ) {
        public HelperInvocationEvidence {
            requireNonBlank(helperClass, "helperClass");
            requireNonBlank(helperMethod, "helperMethod");
            requireNonBlank(invocationExpression, "invocationExpression");
            requireNonBlank(repositoryRelativePath, "repositoryRelativePath");
            if (line < 1) throw new IllegalArgumentException("line must be positive");
            if (column < 1) throw new IllegalArgumentException("column must be positive");
        }
    }

    private static void requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
    }
}
