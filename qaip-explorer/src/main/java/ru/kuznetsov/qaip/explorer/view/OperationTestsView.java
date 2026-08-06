package ru.kuznetsov.qaip.explorer.view;

import java.util.List;
import java.util.Objects;

public record OperationTestsView(
        String repositoryId,
        String operationId,
        OperationVerificationStatus verificationStatus,
        int testCount,
        int checkCount,
        List<OperationTestView> tests
) {
    public OperationTestsView {
        repositoryId = requireText(repositoryId, "repositoryId");
        operationId = requireText(operationId, "operationId");
        Objects.requireNonNull(verificationStatus, "verificationStatus");
        tests = List.copyOf(Objects.requireNonNull(tests, "tests"));
        if (testCount != tests.size()) throw new IllegalArgumentException("testCount must equal tests.size");
        int returnedChecks = tests.stream().mapToInt(OperationTestView::checkCount).sum();
        if (checkCount != returnedChecks) {
            throw new IllegalArgumentException("checkCount must equal returned test checks");
        }
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }
}
