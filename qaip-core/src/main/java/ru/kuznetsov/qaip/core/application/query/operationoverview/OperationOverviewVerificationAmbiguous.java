package ru.kuznetsov.qaip.core.application.query.operationoverview;

public record OperationOverviewVerificationAmbiguous() implements OperationOverviewVerificationSection {
    @Override
    public OperationOverviewVerificationState state() {
        return OperationOverviewVerificationState.AMBIGUOUS;
    }
}
