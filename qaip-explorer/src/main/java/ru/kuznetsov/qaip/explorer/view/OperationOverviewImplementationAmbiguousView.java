package ru.kuznetsov.qaip.explorer.view;

public record OperationOverviewImplementationAmbiguousView(
        OperationOverviewImplementationState state
) implements OperationOverviewImplementationSectionView {
    public OperationOverviewImplementationAmbiguousView {
        if (state != OperationOverviewImplementationState.AMBIGUOUS) {
            throw new IllegalArgumentException("state must be AMBIGUOUS");
        }
    }
}
