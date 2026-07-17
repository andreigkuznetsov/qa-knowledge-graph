package ru.kuznetsov.qaip.analysis.adapter.coverage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.validationcore.QaModelValidationEngine;
import ru.kuznetsov.qaip.core.analysis.AnalysisAssessment;
import ru.kuznetsov.qaip.core.analysis.AnalysisContext;
import ru.kuznetsov.qaip.core.analysis.AssessmentStatus;
import ru.kuznetsov.qaip.core.finding.FindingCategory;
import ru.kuznetsov.qaip.coverage.analyzer.CheckCoverageAnalyzer;
import ru.kuznetsov.qaip.coverage.analyzer.RuleCoverageAnalyzer;
import ru.kuznetsov.qaip.coverage.analyzer.ScenarioCoverageAnalyzer;
import ru.kuznetsov.qaip.coverage.service.CoverageService;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoverageAnalysisEngineTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldAnalyzeValidModelThroughDefaultConstruction() throws Exception {
        CoverageAnalysisEngine engine = new CoverageAnalysisEngine();

        AnalysisAssessment assessment = engine.analyze(
                emptyValidModel(),
                context()
        );

        assertEquals(CoverageAssessmentMapper.ENGINE_ID, engine.id());
        assertEquals(CoverageAssessmentMapper.ENGINE_NAME, engine.name());
        assertEquals(AssessmentStatus.SUCCESS, assessment.status());
        assertTrue(assessment.findings().isEmpty());
        assertEquals(3, assessment.metrics().size());
        assertTrue(
                assessment.metrics().stream()
                        .allMatch(metric -> metric.value() == 100.0)
        );
    }

    @Test
    void shouldMapCoverageGapsToPartialAssessment() throws Exception {
        CoverageAnalysisEngine engine = new CoverageAnalysisEngine();

        AnalysisAssessment assessment = engine.analyze(
                modelWithUncoveredRule(),
                context()
        );

        assertEquals(AssessmentStatus.PARTIAL, assessment.status());
        assertFalse(assessment.findings().isEmpty());
        assertTrue(
                assessment.findings().stream()
                        .allMatch(finding ->
                                finding.category() == FindingCategory.COVERAGE)
        );
        assertTrue(
                assessment.findings().stream()
                        .anyMatch(finding ->
                                "MISSING_SCENARIO".equals(finding.code()))
        );
    }

    @Test
    void shouldSkipInvalidModelWithoutCoverageOutput() throws Exception {
        CoverageAnalysisEngine engine = new CoverageAnalysisEngine();

        AnalysisAssessment assessment = engine.analyze(
                objectMapper.readTree(
                        "{\"schemaVersion\":\"0.1\",\"project\":{}}"
                ),
                context()
        );

        assertEquals(AssessmentStatus.SKIPPED, assessment.status());
        assertTrue(assessment.findings().isEmpty());
        assertTrue(assessment.metrics().isEmpty());
        assertEquals(
                CoverageAssessmentMapper.SKIP_REASON_VALIDATION_FAILED,
                assessment.data().get("reason")
        );
    }

    @Test
    void shouldExecuteWithConstructorSuppliedDependencies() throws Exception {
        CoverageService coverageService = new CoverageService(
                new QaModelValidationEngine(),
                new RuleCoverageAnalyzer(),
                new ScenarioCoverageAnalyzer(),
                new CheckCoverageAnalyzer()
        );
        CoverageAssessmentMapper mapper =
                new CoverageAssessmentMapper();
        CoverageAnalysisEngine engine = new CoverageAnalysisEngine(
                coverageService,
                mapper
        );

        AnalysisAssessment assessment = engine.analyze(
                emptyValidModel(),
                context()
        );

        assertEquals(AssessmentStatus.SUCCESS, assessment.status());
        assertEquals(3, assessment.metrics().size());
    }

    private JsonNode emptyValidModel() throws Exception {
        return objectMapper.readTree("""
                {
                  "schemaVersion": "0.1",
                  "project": {
                    "id": "TEST-PROJECT",
                    "name": "Test project"
                  },
                  "sources": [],
                  "nodes": [],
                  "relationships": []
                }
                """);
    }

    private JsonNode modelWithUncoveredRule() throws Exception {
        return objectMapper.readTree("""
                {
                  "schemaVersion": "0.1",
                  "project": {
                    "id": "TEST-PROJECT",
                    "name": "Test project"
                  },
                  "sources": [],
                  "nodes": [
                    {
                      "id": "BR-001",
                      "type": "BUSINESS_RULE",
                      "name": "Required rule",
                      "status": "DRAFT",
                      "rule": {
                        "code": "REQUIRED_RULE",
                        "ruleType": "VALIDATION_RULE",
                        "text": "Rule must be covered"
                      }
                    }
                  ],
                  "relationships": []
                }
                """);
    }

    private AnalysisContext context() {
        return new AnalysisContext(
                "QAIP-TEST-000007",
                "0.6",
                "EP-0007-02",
                "0.1",
                Instant.parse("2026-07-17T12:00:00Z")
        );
    }
}
