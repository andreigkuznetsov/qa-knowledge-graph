package ru.kuznetsov.qaip.analysis.adapter.coverage;

import ru.kuznetsov.qaip.core.analysis.AnalysisAssessment;
import ru.kuznetsov.qaip.core.analysis.AssessmentStatus;
import ru.kuznetsov.qaip.core.engine.AnalysisResultMapper;
import ru.kuznetsov.qaip.core.finding.AnalysisFinding;
import ru.kuznetsov.qaip.core.finding.FindingCategory;
import ru.kuznetsov.qaip.core.finding.FindingSeverity;
import ru.kuznetsov.qaip.core.metric.AnalysisMetric;
import ru.kuznetsov.qaip.coverage.model.CoverageMetric;
import ru.kuznetsov.qaip.coverage.model.CoverageProblem;
import ru.kuznetsov.qaip.coverage.model.CoverageReport;
import ru.kuznetsov.qaip.coverage.model.CoverageSeverity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class CoverageAssessmentMapper
        implements AnalysisResultMapper<CoverageReport> {

    static final String ENGINE_ID = "coverage";
    static final String ENGINE_NAME = "QA Model Coverage";
    static final String SKIP_REASON_VALIDATION_FAILED =
            "QA_MODEL_VALIDATION_FAILED";

    @Override
    public AnalysisAssessment map(CoverageReport result) {
        Objects.requireNonNull(result, "result must not be null");

        List<AnalysisFinding> findings = result.analyzed()
                ? result.problems().stream()
                        .map(this::mapFinding)
                        .toList()
                : List.of();

        List<AnalysisMetric> metrics = result.metrics().stream()
                .map(this::mapMetric)
                .toList();

        return new AnalysisAssessment(
                ENGINE_ID,
                ENGINE_NAME,
                resolveStatus(result),
                findings,
                metrics,
                data(result)
        );
    }

    private AnalysisFinding mapFinding(CoverageProblem problem) {
        Map<String, Object> details = new LinkedHashMap<>();
        putIfNotNull(details, "objectType", problem.objectType());
        putIfNotNull(details, "objectName", problem.objectName());

        return new AnalysisFinding(
                ENGINE_ID,
                mapSeverity(problem.severity()),
                FindingCategory.COVERAGE,
                problem.type().name(),
                problem.message(),
                problem.objectId(),
                problem.path(),
                problem.explanation(),
                details
        );
    }

    private AnalysisMetric mapMetric(CoverageMetric metric) {
        return new AnalysisMetric(
                ENGINE_ID,
                metric.code().name(),
                metric.name(),
                metric.percentage(),
                "%",
                Map.of(
                        "total", metric.total(),
                        "covered", metric.covered(),
                        "uncovered", metric.uncovered()
                )
        );
    }

    private AssessmentStatus resolveStatus(CoverageReport result) {
        if (!result.analyzed()) {
            return AssessmentStatus.SKIPPED;
        }
        if (!result.problems().isEmpty()) {
            return AssessmentStatus.PARTIAL;
        }
        return AssessmentStatus.SUCCESS;
    }

    private FindingSeverity mapSeverity(CoverageSeverity severity) {
        return switch (severity) {
            case WARNING -> FindingSeverity.WARNING;
        };
    }

    private Map<String, Object> data(CoverageReport result) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("analyzed", result.analyzed());
        putIfNotNull(data, "coverageRelease", result.release());
        putIfNotNull(data, "schemaVersion", result.schemaVersion());
        putIfNotNull(data, "generatedAt", result.generatedAt());
        putIfNotNull(data, "summary", result.summary());
        if (!result.analyzed()) {
            data.put("reason", SKIP_REASON_VALIDATION_FAILED);
        }

        return Map.copyOf(data);
    }

    private void putIfNotNull(
            Map<String, Object> target,
            String key,
            Object value
    ) {
        if (value != null) {
            target.put(key, value);
        }
    }
}
