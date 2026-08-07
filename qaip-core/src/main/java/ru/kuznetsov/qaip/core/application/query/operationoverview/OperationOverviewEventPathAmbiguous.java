package ru.kuznetsov.qaip.core.application.query.operationoverview;

public record OperationOverviewEventPathAmbiguous() implements OperationOverviewEventPathSection {
    @Override
    public OperationOverviewEventPathState state() {
        return OperationOverviewEventPathState.AMBIGUOUS;
    }
}
