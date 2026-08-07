package ru.kuznetsov.qaip.core.application.query.operationoverview;

public record OperationOverviewEventPathNotApplicable() implements OperationOverviewEventPathSection {
    @Override
    public OperationOverviewEventPathState state() {
        return OperationOverviewEventPathState.NOT_APPLICABLE;
    }
}
