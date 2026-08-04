package ru.kuznetsov.qaip.core.application.query.operationdetails;

public sealed interface OperationDetailsQueryResult permits OperationDetailsFound,
        OperationDetailsOperationNotFound, OperationDetailsProjectNotFound, OperationDetailsUnavailable { }
