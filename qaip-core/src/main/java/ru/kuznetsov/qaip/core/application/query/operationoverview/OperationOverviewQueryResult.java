package ru.kuznetsov.qaip.core.application.query.operationoverview;

public sealed interface OperationOverviewQueryResult permits OperationOverviewFound,
        OperationOverviewProjectNotFound, OperationOverviewOperationNotFound { }
