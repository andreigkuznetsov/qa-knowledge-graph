package ru.kuznetsov.qaip.explorer.view;

public record OperationOverviewVerificationAmbiguousView(
        OperationOverviewVerificationState state
) implements OperationOverviewVerificationSectionView {
    public OperationOverviewVerificationAmbiguousView {
        if (state != OperationOverviewVerificationState.AMBIGUOUS) {
            throw new IllegalArgumentException("state must be AMBIGUOUS");
        }
    }
}
