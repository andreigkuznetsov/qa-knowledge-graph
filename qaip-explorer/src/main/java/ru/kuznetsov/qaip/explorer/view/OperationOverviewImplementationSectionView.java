package ru.kuznetsov.qaip.explorer.view;

public sealed interface OperationOverviewImplementationSectionView
        permits OperationOverviewImplementationAvailableView,
        OperationOverviewImplementationIncompleteView,
        OperationOverviewImplementationAmbiguousView {
    OperationOverviewImplementationState state();
}
