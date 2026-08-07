package ru.kuznetsov.qaip.core.application.query.operationoverview;

import ru.kuznetsov.qaip.core.application.query.operationtests.OperationVerificationStatus;
import ru.kuznetsov.qaip.core.application.query.operationtests.OperationVerificationSemantics;
import ru.kuznetsov.qaip.core.application.query.operationtests.QualifiedOperationTest;

import java.util.List;

public record OperationOverviewVerificationAvailable(
        OperationVerificationStatus verificationStatus,
        int testCount,
        int checkCount,
        List<QualifiedOperationTest> tests
) implements OperationOverviewVerificationSection {
    public OperationOverviewVerificationAvailable {
        tests = OperationVerificationSemantics.preserve(
                verificationStatus, testCount, checkCount, tests).tests();
    }

    @Override
    public OperationOverviewVerificationState state() {
        return OperationOverviewVerificationState.AVAILABLE;
    }
}
