package ru.kuznetsov.qaip.core.application.query.operationoverview;

public record OperationOverviewImplementationAmbiguous() implements OperationOverviewImplementationSection {
    @Override
    public OperationOverviewImplementationState state() {
        return OperationOverviewImplementationState.AMBIGUOUS;
    }
}
