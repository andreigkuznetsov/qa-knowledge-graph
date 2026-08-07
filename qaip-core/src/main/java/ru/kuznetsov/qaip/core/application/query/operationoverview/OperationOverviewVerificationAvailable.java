package ru.kuznetsov.qaip.core.application.query.operationoverview;

import ru.kuznetsov.qaip.core.application.query.operationtests.OperationVerificationStatus;
import ru.kuznetsov.qaip.core.application.query.operationtests.QualifiedOperationTest;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record OperationOverviewVerificationAvailable(
        OperationVerificationStatus verificationStatus,
        int testCount,
        int checkCount,
        List<QualifiedOperationTest> tests
) implements OperationOverviewVerificationSection {
    public OperationOverviewVerificationAvailable {
        Objects.requireNonNull(verificationStatus, "verificationStatus");
        tests = Objects.requireNonNull(tests, "tests").stream()
                .sorted(Comparator.comparing(QualifiedOperationTest::testId))
                .toList();
        if (testCount != tests.size()) {
            throw new IllegalArgumentException("testCount must equal tests.size");
        }
        int returnedChecks = tests.stream().mapToInt(QualifiedOperationTest::qualifiedCheckCount).sum();
        if (checkCount != returnedChecks) {
            throw new IllegalArgumentException("checkCount must equal returned test checks");
        }
        boolean validStatus = switch (verificationStatus) {
            case VERIFIED -> testCount > 0 && checkCount > 0;
            case PARTIALLY_VERIFIED -> testCount > 0 && checkCount == 0;
            case UNVERIFIED -> testCount == 0 && checkCount == 0;
        };
        if (!validStatus) {
            throw new IllegalArgumentException("verificationStatus must match returned tests and checks");
        }
    }

    @Override
    public OperationOverviewVerificationState state() {
        return OperationOverviewVerificationState.AVAILABLE;
    }
}
