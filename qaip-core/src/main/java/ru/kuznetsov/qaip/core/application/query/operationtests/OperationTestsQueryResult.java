package ru.kuznetsov.qaip.core.application.query.operationtests;

public sealed interface OperationTestsQueryResult permits OperationTestsFound,
        OperationTestsProjectNotFound, OperationTestsOperationNotFound,
        OperationTestsNoneQualified, OperationTestsAmbiguous { }
