package ru.kuznetsov.qaip.explorer.view;

import java.util.Objects;

public record OperationOverviewEventPathAvailableView(
        OperationOverviewEventPathState state,
        EventPathView path
) implements OperationOverviewEventPathSectionView {
    public OperationOverviewEventPathAvailableView {
        if (state != OperationOverviewEventPathState.AVAILABLE) {
            throw new IllegalArgumentException("state must be AVAILABLE");
        }
        Objects.requireNonNull(path, "path");
    }
}
