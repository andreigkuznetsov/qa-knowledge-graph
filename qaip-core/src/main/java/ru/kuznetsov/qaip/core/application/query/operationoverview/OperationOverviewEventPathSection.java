package ru.kuznetsov.qaip.core.application.query.operationoverview;

public sealed interface OperationOverviewEventPathSection permits OperationOverviewEventPathAvailable,
        OperationOverviewEventPathNotApplicable, OperationOverviewEventPathIncomplete,
        OperationOverviewEventPathAmbiguous {
    OperationOverviewEventPathState state();
}
