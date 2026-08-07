package ru.kuznetsov.qaip.core.application.query.operationtests;

public enum OperationVerificationStatus {
    VERIFIED,
    PARTIALLY_VERIFIED,
    UNVERIFIED;

    static OperationVerificationStatus fromCounts(int testCount, int checkCount) {
        return OperationVerificationSemantics.status(testCount, checkCount);
    }
}
