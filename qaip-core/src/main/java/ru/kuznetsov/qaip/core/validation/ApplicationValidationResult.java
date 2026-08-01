package ru.kuznetsov.qaip.core.validation;

public sealed interface ApplicationValidationResult
        permits ApplicationValidationSuccess, ApplicationValidationFailure { }
