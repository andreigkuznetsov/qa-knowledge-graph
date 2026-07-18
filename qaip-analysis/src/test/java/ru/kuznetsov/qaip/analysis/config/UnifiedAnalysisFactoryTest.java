package ru.kuznetsov.qaip.analysis.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.analysis.orchestration.UnifiedAnalysisResult;
import ru.kuznetsov.qaip.analysis.service.UnifiedAnalysisService;
import ru.kuznetsov.qaip.core.analysis.AssessmentStatus;
import ru.kuznetsov.qaip.core.metadata.AnalysisIdGenerator;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnifiedAnalysisFactoryTest {

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    @Test
    void defaultFactoryShouldExecuteCompletePipelineForFullyCoveredModel()
            throws Exception {

        UnifiedAnalysisService service =
                UnifiedAnalysisFactory.createDefault();

        UnifiedAnalysisResult result =
                service.analyze(emptyValidModel());

        assertEquals(
                UnifiedAnalysisFactory.RELEASE,
                result.metadata().release()
        );
        assertEquals(
                UnifiedAnalysisFactory.SCHEMA_VERSION,
                result.metadata().schemaVersion()
        );
        assertEquals(
                2,
                result.execution()
                        .engineResults()
                        .size()
        );
        assertEquals(
                "validation",
                result.execution()
                        .engineResults()
                        .get(0)
                        .engineId()
        );
        assertEquals(
                AssessmentStatus.SUCCESS,
                result.execution()
                        .engineResults()
                        .get(0)
                        .assessment()
                        .status()
        );
        assertEquals(
                "coverage",
                result.execution()
                        .engineResults()
                        .get(1)
                        .engineId()
        );
        assertEquals(
                AssessmentStatus.SUCCESS,
                result.execution()
                        .engineResults()
                        .get(1)
                        .assessment()
                        .status()
        );
    }

    @Test
    void defaultFactoryShouldReportPartialCoverageForValidModel()
            throws Exception {

        UnifiedAnalysisResult result = UnifiedAnalysisFactory
                .createDefault()
                .analyze(modelWithUncoveredRule());

        assertEquals(
                "validation",
                result.execution().engineResults().get(0).engineId()
        );
        assertEquals(
                AssessmentStatus.PARTIAL,
                result.execution().engineResults().get(0).assessment().status()
        );
        assertEquals(
                true,
                result.execution().engineResults().get(0)
                        .assessment().data().get("valid")
        );
        assertEquals(
                "coverage",
                result.execution().engineResults().get(1).engineId()
        );
        assertEquals(
                AssessmentStatus.PARTIAL,
                result.execution().engineResults().get(1).assessment().status()
        );
        assertFalse(
                result.execution().engineResults().get(1)
                        .assessment().findings().isEmpty()
        );
    }

    @Test
    void defaultFactoryShouldSkipCoverageForInvalidModel()
            throws Exception {

        UnifiedAnalysisResult result = UnifiedAnalysisFactory
                .createDefault()
                .analyze(objectMapper.readTree(
                        "{\"schemaVersion\":\"0.1\",\"project\":{}}"
                ));

        var validation = result.execution().engineResults().get(0);
        var coverage = result.execution().engineResults().get(1);

        assertEquals("validation", validation.engineId());
        assertEquals(AssessmentStatus.FAILED, validation.assessment().status());
        assertFalse(validation.assessment().findings().isEmpty());
        assertEquals("coverage", coverage.engineId());
        assertEquals(AssessmentStatus.SKIPPED, coverage.assessment().status());
        assertTrue(coverage.assessment().findings().isEmpty());
        assertTrue(coverage.assessment().metrics().isEmpty());
    }

    @Test
    void customFactoryShouldSupportEmptyRegistry()
            throws Exception {

        AnalysisIdGenerator idGenerator =
                () -> "QAIP-TEST-000004";

        UnifiedAnalysisService service =
                UnifiedAnalysisFactory.create(
                        List.of(),
                        idGenerator
                );

        JsonNode model =
                objectMapper.readTree(
                        "{\"nodes\":[],\"relationships\":[]}"
                );

        UnifiedAnalysisResult result =
                service.analyze(model);

        assertEquals(
                "QAIP-TEST-000004",
                result.metadata().analysisId()
        );
        assertTrue(
                result.execution()
                        .engineResults()
                        .isEmpty()
        );
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
}
