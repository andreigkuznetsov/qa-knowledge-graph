package ru.kuznetsov.qaip.explorer.application;

import ru.kuznetsov.qaip.explorer.view.OperationOverviewView;

import java.util.Objects;

public record OperationOverviewProjectionFound(
        OperationOverviewView overview
) implements OperationOverviewProjectionResult {
    public OperationOverviewProjectionFound {
        Objects.requireNonNull(overview, "overview");
    }
}
