package ru.kuznetsov.qaip.core.application.query.operationtests;

import java.util.List;
import java.util.Objects;
import java.util.Comparator;

public record OperationTestsFound(
        String projectId,
        String operationId,
        OperationVerificationStatus verificationStatus,
        List<QualifiedOperationTest> tests
) implements OperationTestsQueryResult {
    public OperationTestsFound {
        projectId = OperationTestsProjectNotFound.requireId(projectId, "projectId");
        operationId = OperationTestsProjectNotFound.requireId(operationId, "operationId");
        Objects.requireNonNull(verificationStatus, "verificationStatus");
        tests = Objects.requireNonNull(tests, "tests").stream()
                .sorted(Comparator.comparing(QualifiedOperationTest::testId))
                .toList();
        if (tests.isEmpty()) throw new IllegalArgumentException("tests must not be empty");
        int checkCount = tests.stream().mapToInt(QualifiedOperationTest::qualifiedCheckCount).sum();
        if (verificationStatus != OperationVerificationStatus.fromCounts(tests.size(), checkCount)) {
            throw new IllegalArgumentException("verificationStatus must derive from returned tests and checks");
        }
    }

    public int testCount() {
        return tests.size();
    }

    public int checkCount() {
        return tests.stream().mapToInt(QualifiedOperationTest::qualifiedCheckCount).sum();
    }
}
