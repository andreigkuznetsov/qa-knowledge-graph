package ru.kuznetsov.qaip.explorer.adapter.runtime;

import ru.kuznetsov.qaip.explorer.view.OperationVerificationStatus;

final class OperationVerificationStatusMapper {
    private OperationVerificationStatusMapper() { }

    static OperationVerificationStatus fromCounts(int testCount, int checkCount) {
        if (testCount > 0 && checkCount > 0) return OperationVerificationStatus.VERIFIED;
        if (testCount > 0 || checkCount > 0) return OperationVerificationStatus.PARTIALLY_VERIFIED;
        return OperationVerificationStatus.UNVERIFIED;
    }
}
