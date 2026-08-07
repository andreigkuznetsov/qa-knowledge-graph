package ru.kuznetsov.qaip.core.application.query.operationoverview;

import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathResult;

import java.util.Objects;

public record OperationOverviewEventPathAvailable(
        EventPathResult path
) implements OperationOverviewEventPathSection {
    public OperationOverviewEventPathAvailable {
        Objects.requireNonNull(path, "path");
    }

    @Override
    public OperationOverviewEventPathState state() {
        return OperationOverviewEventPathState.AVAILABLE;
    }
}
