package ru.kuznetsov.qaip.core.application.query.operationtests;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Authoritative Runtime invariant for verification status, counts, and evidence ordering. */
public final class OperationVerificationSemantics {
    private OperationVerificationSemantics() { }

    public static Summary summarize(List<QualifiedOperationTest> tests) {
        List<QualifiedOperationTest> ordered = Objects.requireNonNull(tests, "tests").stream()
                .sorted(Comparator.comparing(QualifiedOperationTest::testId))
                .toList();
        int testCount = ordered.size();
        int checkCount = ordered.stream().mapToInt(QualifiedOperationTest::qualifiedCheckCount).sum();
        return new Summary(status(testCount, checkCount), testCount, checkCount, ordered);
    }

    public static Summary preserve(
            OperationVerificationStatus verificationStatus,
            int testCount,
            int checkCount,
            List<QualifiedOperationTest> tests
    ) {
        Objects.requireNonNull(verificationStatus, "verificationStatus");
        Summary summary = summarize(tests);
        if (testCount != summary.testCount()) {
            throw new IllegalArgumentException("testCount must equal tests.size");
        }
        if (checkCount != summary.checkCount()) {
            throw new IllegalArgumentException("checkCount must equal returned test checks");
        }
        if (verificationStatus != summary.verificationStatus()) {
            throw new IllegalArgumentException("verificationStatus must match returned tests and checks");
        }
        return summary;
    }

    static OperationVerificationStatus status(int testCount, int checkCount) {
        if (testCount > 0 && checkCount > 0) return OperationVerificationStatus.VERIFIED;
        if (testCount > 0 || checkCount > 0) return OperationVerificationStatus.PARTIALLY_VERIFIED;
        return OperationVerificationStatus.UNVERIFIED;
    }

    public static final class Summary {
        private final OperationVerificationStatus verificationStatus;
        private final int testCount;
        private final int checkCount;
        private final List<QualifiedOperationTest> tests;

        private Summary(
                OperationVerificationStatus verificationStatus,
                int testCount,
                int checkCount,
                List<QualifiedOperationTest> tests
        ) {
            this.verificationStatus = Objects.requireNonNull(verificationStatus, "verificationStatus");
            this.testCount = testCount;
            this.checkCount = checkCount;
            this.tests = List.copyOf(Objects.requireNonNull(tests, "tests"));
        }

        public OperationVerificationStatus verificationStatus() {
            return verificationStatus;
        }

        public int testCount() {
            return testCount;
        }

        public int checkCount() {
            return checkCount;
        }

        public List<QualifiedOperationTest> tests() {
            return tests;
        }
    }
}
