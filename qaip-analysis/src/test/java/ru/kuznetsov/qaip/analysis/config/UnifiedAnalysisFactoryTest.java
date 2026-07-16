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
    void defaultFactoryShouldExecuteValidationEngine()
            throws Exception {

        UnifiedAnalysisService service =
                UnifiedAnalysisFactory.createDefault();

        JsonNode invalidModel =
                objectMapper.readTree(
                        "{\"schemaVersion\":\"0.1\"}"
                );

        UnifiedAnalysisResult result =
                service.analyze(invalidModel);

        assertEquals(
                UnifiedAnalysisFactory.RELEASE,
                result.metadata().release()
        );
        assertEquals(
                UnifiedAnalysisFactory.SCHEMA_VERSION,
                result.metadata().schemaVersion()
        );
        assertEquals(
                1,
                result.execution()
                        .engineResults()
                        .size()
        );
        assertEquals(
                "validation",
                result.execution()
                        .engineResults()
                        .getFirst()
                        .engineId()
        );
        assertEquals(
                AssessmentStatus.FAILED,
                result.execution()
                        .engineResults()
                        .getFirst()
                        .assessment()
                        .status()
        );
        assertFalse(
                result.execution()
                        .engineResults()
                        .getFirst()
                        .assessment()
                        .findings()
                        .isEmpty()
        );
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
}
