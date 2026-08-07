package ru.kuznetsov.qaip.explorer.view;

public record OperationOverviewEventPathAmbiguousView(
        OperationOverviewEventPathState state
) implements OperationOverviewEventPathSectionView {
    public OperationOverviewEventPathAmbiguousView {
        if (state != OperationOverviewEventPathState.AMBIGUOUS) {
            throw new IllegalArgumentException("state must be AMBIGUOUS");
        }
    }
}
