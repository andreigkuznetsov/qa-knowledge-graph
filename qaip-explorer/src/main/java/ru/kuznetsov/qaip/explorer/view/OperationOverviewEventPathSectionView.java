package ru.kuznetsov.qaip.explorer.view;

public sealed interface OperationOverviewEventPathSectionView
        permits OperationOverviewEventPathAvailableView,
        OperationOverviewEventPathNotApplicableView,
        OperationOverviewEventPathIncompleteView,
        OperationOverviewEventPathAmbiguousView {
    OperationOverviewEventPathState state();
}
