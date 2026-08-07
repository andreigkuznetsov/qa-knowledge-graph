package ru.kuznetsov.qaip.explorer.view;

public record OperationOverviewImplementationIncompleteView(
        OperationOverviewImplementationState state
) implements OperationOverviewImplementationSectionView {
    public OperationOverviewImplementationIncompleteView {
        if (state != OperationOverviewImplementationState.INCOMPLETE) {
            throw new IllegalArgumentException("state must be INCOMPLETE");
        }
    }
}
