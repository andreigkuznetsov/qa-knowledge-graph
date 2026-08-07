package ru.kuznetsov.qaip.core.application.query.operationoverview;

public sealed interface OperationOverviewImplementationSection permits OperationOverviewImplementationAvailable,
        OperationOverviewImplementationIncomplete, OperationOverviewImplementationAmbiguous {
    OperationOverviewImplementationState state();
}
