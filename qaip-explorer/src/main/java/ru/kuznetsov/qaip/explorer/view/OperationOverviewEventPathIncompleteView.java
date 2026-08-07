package ru.kuznetsov.qaip.explorer.view;

public record OperationOverviewEventPathIncompleteView(
        OperationOverviewEventPathState state
) implements OperationOverviewEventPathSectionView {
    public OperationOverviewEventPathIncompleteView {
        if (state != OperationOverviewEventPathState.INCOMPLETE) {
            throw new IllegalArgumentException("state must be INCOMPLETE");
        }
    }
}
