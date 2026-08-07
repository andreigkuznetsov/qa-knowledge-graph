package ru.kuznetsov.qaip.explorer.view;

public sealed interface OperationOverviewVerificationSectionView
        permits OperationOverviewVerificationAvailableView,
        OperationOverviewVerificationAmbiguousView {
    OperationOverviewVerificationState state();
}
