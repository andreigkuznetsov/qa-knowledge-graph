package ru.kuznetsov.qaip.core.application.query.validation;

import ru.kuznetsov.qaip.core.application.validation.ValidationEngine;
import ru.kuznetsov.qaip.core.persistence.read.ProjectReader;

import java.util.Objects;

public final class DefaultValidationUseCase implements ValidationUseCase {
    private final ProjectReader projectReader;
    private final ValidationEngine validationEngine;
    private final ValidationReportMapper validationReportMapper;

    public DefaultValidationUseCase(ProjectReader projectReader, ValidationEngine validationEngine,
                                    ValidationReportMapper validationReportMapper) {
        this.projectReader = Objects.requireNonNull(projectReader, "projectReader");
        this.validationEngine = Objects.requireNonNull(validationEngine, "validationEngine");
        this.validationReportMapper = Objects.requireNonNull(validationReportMapper, "validationReportMapper");
    }

    @Override
    public ValidationQueryResult execute(String projectId) {
        Objects.requireNonNull(projectId, "projectId");
        if (projectId.isBlank()) throw new IllegalArgumentException("projectId must not be blank");

        var projectResult = Objects.requireNonNull(projectReader.findById(projectId), "project reader result");
        if (projectResult.isEmpty()) return new ValidationProjectNotFound(projectId);

        var report = Objects.requireNonNull(
                validationEngine.validate(projectResult.orElseThrow()), "validation engine result");
        var mappedReport = Objects.requireNonNull(
                validationReportMapper.map(report), "validation report mapper result");
        return new ValidationCompleted(projectId, mappedReport);
    }
}
