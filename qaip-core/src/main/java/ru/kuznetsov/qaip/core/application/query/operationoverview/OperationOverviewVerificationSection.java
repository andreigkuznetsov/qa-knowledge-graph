package ru.kuznetsov.qaip.core.application.query.operationoverview;

public sealed interface OperationOverviewVerificationSection permits OperationOverviewVerificationAvailable,
        OperationOverviewVerificationAmbiguous {
    OperationOverviewVerificationState state();
}
