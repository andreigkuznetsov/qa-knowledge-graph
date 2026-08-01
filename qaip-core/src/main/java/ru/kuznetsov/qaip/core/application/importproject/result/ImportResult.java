package ru.kuznetsov.qaip.core.application.importproject.result;

public sealed interface ImportResult permits ImportCompletedResult, ImportRejectedResult,
        ImportPersistenceRejectedResult, ImportPersistenceFailedResult { }
