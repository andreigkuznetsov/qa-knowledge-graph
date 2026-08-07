package ru.kuznetsov.qaip.core.application.query.operationoverview;

public record OperationOverviewEventPathIncomplete() implements OperationOverviewEventPathSection {
    @Override
    public OperationOverviewEventPathState state() {
        return OperationOverviewEventPathState.INCOMPLETE;
    }
}
