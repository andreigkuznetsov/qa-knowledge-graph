package ru.kuznetsov.qaip.core.application.query.operationtests;

public enum OperationVerificationStatus {
    VERIFIED,
    PARTIALLY_VERIFIED,
    UNVERIFIED;

    static OperationVerificationStatus fromCounts(int testCount, int checkCount) {
        if (testCount > 0 && checkCount > 0) return VERIFIED;
        if (testCount > 0 || checkCount > 0) return PARTIALLY_VERIFIED;
        return UNVERIFIED;
    }
}
