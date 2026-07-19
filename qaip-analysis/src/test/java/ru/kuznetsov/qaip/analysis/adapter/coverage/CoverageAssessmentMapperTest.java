package ru.kuznetsov.qaip.analysis.adapter.coverage;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;
import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;
import ru.kuznetsov.qagraph.validationcore.model.ValidationSummary;
import ru.kuznetsov.qaip.core.analysis.AnalysisAssessment;
import ru.kuznetsov.qaip.core.analysis.AssessmentStatus;
import ru.kuznetsov.qaip.core.finding.FindingCategory;
import ru.kuznetsov.qaip.core.finding.FindingSeverity;
import ru.kuznetsov.qaip.coverage.model.CoverageMetric;
import ru.kuznetsov.qaip.coverage.model.CoverageMetricCode;
import ru.kuznetsov.qaip.coverage.model.CoverageProblem;
import ru.kuznetsov.qaip.coverage.model.CoverageProblemType;
import ru.kuznetsov.qaip.coverage.model.CoverageReport;
import ru.kuznetsov.qaip.coverage.model.CoverageSeverity;
import ru.kuznetsov.qaip.coverage.model.CoverageSummary;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoverageAssessmentMapperTest {

    private final CoverageAssessmentMapper mapper =
            new CoverageAssessmentMapper();

    @Test
    void shouldMapFullyCoveredReportToSuccess() {
        CoverageSummary summary = summary(0);
        CoverageReport report = report(
                true,
                summary,
                List.of(
                        metric("RULE_SCENARIO_COVERAGE", 100.0, 2, 2, 0),
                        metric("SCENARIO_TEST_COVERAGE", 66.67, 3, 2, 1)
                ),
                List.of(),
                validValidation()
        );

        AnalysisAssessment assessment = mapper.map(report);

        assertEquals(CoverageAssessmentMapper.ENGINE_ID, assessment.engineId());
        assertEquals(CoverageAssessmentMapper.ENGINE_NAME, assessment.engineName());
        assertEquals(AssessmentStatus.SUCCESS, assessment.status());
        assertTrue(assessment.findings().isEmpty());
        assertEquals(2, assessment.metrics().size());
        assertEquals(100.0, assessment.metrics().get(0).value());
        assertEquals(66.67, assessment.metrics().get(1).value());
        assertEquals("%", assessment.metrics().get(1).unit());
        assertEquals(3, assessment.metrics().get(1).details().get("total"));
        assertEquals(2, assessment.metrics().get(1).details().get("covered"));
        assertEquals(1, assessment.metrics().get(1).details().get("uncovered"));
        assertEquals(true, assessment.data().get("analyzed"));
        assertSame(summary, assessment.data().get("summary"));
    }

    @Test
    void shouldMapCoverageProblemsToPartialFindingsInSourceOrder() {
        CoverageProblem missingScenario = problem(
                CoverageProblemType.MISSING_SCENARIO,
                "BR-002",
                "BUSINESS_RULE",
                "Правило закрытия",
                "Для правила отсутствует сценарий",
                "Добавьте покрывающий BDD-сценарий",
                "/nodes/3"
        );
        CoverageProblem missingCheck = problem(
                CoverageProblemType.MISSING_CHECK,
                "TEST-001",
                "TEST_IMPLEMENTATION",
                "Проверка закрытия",
                "Для теста отсутствует проверка",
                "Добавьте атомарную проверку",
                "/nodes/8"
        );
        CoverageReport report = report(
                true,
                summary(2),
                List.of(
                        metric("RULE_SCENARIO_COVERAGE", 50.0, 2, 1, 1),
                        metric("TEST_CHECK_COVERAGE", 0.0, 1, 0, 1)
                ),
                List.of(missingScenario, missingCheck),
                validValidation()
        );

        AnalysisAssessment assessment = mapper.map(report);

        assertEquals(AssessmentStatus.PARTIAL, assessment.status());
        assertEquals(2, assessment.findings().size());
        assertEquals("MISSING_SCENARIO", assessment.findings().get(0).code());
        assertEquals("MISSING_CHECK", assessment.findings().get(1).code());
        assertEquals(
                FindingCategory.COVERAGE,
                assessment.findings().get(0).category()
        );
        assertEquals(
                FindingSeverity.WARNING,
                assessment.findings().get(0).severity()
        );
        assertEquals(
                "Для правила отсутствует сценарий",
                assessment.findings().get(0).message()
        );
        assertEquals("BR-002", assessment.findings().get(0).objectId());
        assertEquals("/nodes/3", assessment.findings().get(0).path());
        assertEquals(
                "Добавьте покрывающий BDD-сценарий",
                assessment.findings().get(0).recommendation()
        );
        assertEquals(
                "BUSINESS_RULE",
                assessment.findings().get(0).details().get("objectType")
        );
        assertEquals(
                "Правило закрытия",
                assessment.findings().get(0).details().get("objectName")
        );
        assertEquals(
                "RULE_SCENARIO_COVERAGE",
                assessment.metrics().get(0).code()
        );
        assertEquals(
                "TEST_CHECK_COVERAGE",
                assessment.metrics().get(1).code()
        );
    }

    @Test
    void shouldSkipNotAnalyzedReportWithoutCopyingValidationFindings() {
        QaModelValidationResult validation = new QaModelValidationResult(
                false,
                "0.1",
                new ValidationSummary(1, 0, 1),
                List.of(
                        ValidationIssue.schemaError(
                                "REQUIRED",
                                "Отсутствует поле nodes",
                                "$"
                        )
                )
        );
        CoverageReport report = report(
                false,
                null,
                List.of(),
                List.of(),
                validation
        );

        AnalysisAssessment assessment = mapper.map(report);

        assertEquals(AssessmentStatus.SKIPPED, assessment.status());
        assertTrue(assessment.findings().isEmpty());
        assertTrue(assessment.metrics().isEmpty());
        assertEquals(false, assessment.data().get("analyzed"));
        assertEquals(
                CoverageAssessmentMapper.SKIP_REASON_VALIDATION_FAILED,
                assessment.data().get("reason")
        );
        assertEquals("0.4", assessment.data().get("coverageRelease"));
        assertEquals("0.1", assessment.data().get("schemaVersion"));
    }

    @Test
    void shouldRejectNullResult() {
        assertThrows(
                NullPointerException.class,
                () -> mapper.map(null)
        );
    }

    @Test
    void shouldOmitNullableMetadataAndFindingDetails() {
        CoverageProblem problem = problem(
                CoverageProblemType.MISSING_SCENARIO,
                "BR-002",
                null,
                null,
                "Для правила отсутствует сценарий",
                null,
                null
        );
        CoverageReport report = new CoverageReport(
                true,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(problem),
                validValidation()
        );

        AnalysisAssessment assessment = mapper.map(report);

        assertEquals(AssessmentStatus.PARTIAL, assessment.status());
        assertTrue(assessment.findings().getFirst().details().isEmpty());
        assertEquals(Map.of("analyzed", true), assessment.data());
    }

    private CoverageReport report(
            boolean analyzed,
            CoverageSummary summary,
            List<CoverageMetric> metrics,
            List<CoverageProblem> problems,
            QaModelValidationResult validation
    ) {
        return new CoverageReport(
                analyzed,
                "0.4",
                "0.1",
                Instant.parse("2026-07-17T12:00:00Z"),
                summary,
                metrics,
                problems,
                validation
        );
    }

    private CoverageMetric metric(
            String code,
            double percentage,
            int total,
            int covered,
            int uncovered
    ) {
        return new CoverageMetric(
                CoverageMetricCode.valueOf(code),
                code,
                total,
                covered,
                uncovered,
                percentage
        );
    }

    private CoverageProblem problem(
            CoverageProblemType type,
            String objectId,
            String objectType,
            String objectName,
            String message,
            String explanation,
            String path
    ) {
        return new CoverageProblem(
                type,
                CoverageSeverity.WARNING,
                objectId,
                objectType,
                objectName,
                message,
                explanation,
                path
        );
    }

    private CoverageSummary summary(int problems) {
        return new CoverageSummary(
                2,
                2,
                0,
                100.0,
                3,
                2,
                1,
                66.67,
                1,
                1,
                0,
                100.0,
                problems
        );
    }

    private QaModelValidationResult validValidation() {
        return new QaModelValidationResult(
                true,
                "0.1",
                new ValidationSummary(0, 0, 0),
                List.of()
        );
    }
}
