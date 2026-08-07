package ru.kuznetsov.qaip.core.application.query.operationoverview;

import java.util.Objects;

public record OperationOverviewImplementationAvailable(
        OperationOverviewImplementation implementation
) implements OperationOverviewImplementationSection {
    public OperationOverviewImplementationAvailable {
        Objects.requireNonNull(implementation, "implementation");
    }

    @Override
    public OperationOverviewImplementationState state() {
        return OperationOverviewImplementationState.AVAILABLE;
    }
}
