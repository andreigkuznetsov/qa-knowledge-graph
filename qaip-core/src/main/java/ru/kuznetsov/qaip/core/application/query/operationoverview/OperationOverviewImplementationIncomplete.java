package ru.kuznetsov.qaip.core.application.query.operationoverview;

public record OperationOverviewImplementationIncomplete() implements OperationOverviewImplementationSection {
    @Override
    public OperationOverviewImplementationState state() {
        return OperationOverviewImplementationState.INCOMPLETE;
    }
}
