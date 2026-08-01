package ru.kuznetsov.qaip.core.application.query.validation;

public interface ValidationUseCase {
    ValidationQueryResult execute(String projectId);
}
