package ru.kuznetsov.qaip.explorer.view;

public record OperationOverviewEventPathNotApplicableView(
        OperationOverviewEventPathState state
) implements OperationOverviewEventPathSectionView {
    public OperationOverviewEventPathNotApplicableView {
        if (state != OperationOverviewEventPathState.NOT_APPLICABLE) {
            throw new IllegalArgumentException("state must be NOT_APPLICABLE");
        }
    }
}
