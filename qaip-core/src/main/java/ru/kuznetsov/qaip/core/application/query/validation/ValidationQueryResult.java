package ru.kuznetsov.qaip.core.application.query.validation;

public sealed interface ValidationQueryResult
        permits ValidationCompleted, ValidationProjectNotFound { }
