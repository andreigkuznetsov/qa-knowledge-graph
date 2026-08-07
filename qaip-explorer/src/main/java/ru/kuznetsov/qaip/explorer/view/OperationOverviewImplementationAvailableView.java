package ru.kuznetsov.qaip.explorer.view;

import java.util.Objects;

public record OperationOverviewImplementationAvailableView(
        OperationOverviewImplementationState state,
        ImplementationPathView path
) implements OperationOverviewImplementationSectionView {
    public OperationOverviewImplementationAvailableView {
        if (state != OperationOverviewImplementationState.AVAILABLE) {
            throw new IllegalArgumentException("state must be AVAILABLE");
        }
        Objects.requireNonNull(path, "path");
    }
}
