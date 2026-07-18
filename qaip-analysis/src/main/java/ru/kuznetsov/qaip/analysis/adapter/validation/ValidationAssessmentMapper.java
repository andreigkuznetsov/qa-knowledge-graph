package ru.kuznetsov.qaip.analysis.adapter.validation;

import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;
import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;
import ru.kuznetsov.qagraph.validationcore.model.ValidationSeverity;
import ru.kuznetsov.qaip.core.analysis.AnalysisAssessment;
import ru.kuznetsov.qaip.core.analysis.AssessmentStatus;
import ru.kuznetsov.qaip.core.finding.AnalysisFinding;
import ru.kuznetsov.qaip.core.finding.FindingCategory;
import ru.kuznetsov.qaip.core.finding.FindingSeverity;
import ru.kuznetsov.qaip.core.engine.AnalysisResultMapper;
import ru.kuznetsov.qaip.core.metric.AnalysisMetric;

import java.util.List;
import java.util.Map;
import java.util.Objects;

final class ValidationAssessmentMapper
        implements AnalysisResultMapper<QaModelValidationResult> {

    static final String ENGINE_ID = "validation";
    static final String ENGINE_NAME = "QA Model Validation";

    @Override
    public AnalysisAssessment map(QaModelValidationResult result) {
        Objects.requireNonNull(result, "result must not be null");

        List<AnalysisFinding> findings = result.issues().stream()
                .map(this::mapFinding)
                .toList();

        List<AnalysisMetric> metrics = List.of(
                metric("VALIDATION_ERRORS", "Ошибки валидации", result.summary().errors()),
                metric("VALIDATION_WARNINGS", "Предупреждения валидации", result.summary().warnings()),
                metric("VALIDATION_ISSUES", "Всего замечаний валидации", result.summary().total())
        );

        return new AnalysisAssessment(
                ENGINE_ID,
                ENGINE_NAME,
                resolveStatus(result),
                findings,
                metrics,
                Map.of(
                        "valid", result.valid(),
                        "schemaVersion", result.schemaVersion() == null ? "" : result.schemaVersion()
                )
        );
    }

    private AnalysisFinding mapFinding(ValidationIssue issue) {
        return new AnalysisFinding(
                ENGINE_ID,
                mapSeverity(issue.severity()),
                FindingCategory.VALIDATION,
                issue.code(),
                issue.message(),
                issue.objectId(),
                issue.path(),
                null,
                Map.of("layer", issue.layer().name())
        );
    }

    private AnalysisMetric metric(String code, String name, int value) {
        return new AnalysisMetric(
                ENGINE_ID,
                code,
                name,
                value,
                "count",
                Map.of()
        );
    }

    private AssessmentStatus resolveStatus(QaModelValidationResult result) {
        if (!result.valid()) {
            return AssessmentStatus.FAILED;
        }
        if (result.summary().warnings() > 0) {
            return AssessmentStatus.PARTIAL;
        }
        return AssessmentStatus.SUCCESS;
    }

    private FindingSeverity mapSeverity(ValidationSeverity severity) {
        return switch (severity) {
            case ERROR -> FindingSeverity.ERROR;
            case WARNING -> FindingSeverity.WARNING;
        };
    }
}
