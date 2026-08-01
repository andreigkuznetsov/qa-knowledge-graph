package ru.kuznetsov.qaip.core.application.importproject;

public sealed interface ImportProjectUseCaseResult permits ImportProjectCompleted,
        ImportProjectRejected, ImportProjectPersistenceRejected, ImportProjectPersistenceFailed { }
