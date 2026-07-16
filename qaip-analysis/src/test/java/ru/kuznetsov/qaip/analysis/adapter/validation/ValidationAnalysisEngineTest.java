package ru.kuznetsov.qaip.analysis.adapter.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.analysis.AnalysisAssessment;
import ru.kuznetsov.qaip.core.analysis.AnalysisContext;
import ru.kuznetsov.qaip.core.analysis.AssessmentStatus;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ValidationAnalysisEngineTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldValidateDocumentThroughCoreEngine() throws Exception {
        ValidationAnalysisEngine engine = new ValidationAnalysisEngine();

        JsonNode invalidModel = objectMapper.readTree(
                "{\"schemaVersion\":\"0.1\"}"
        );

        AnalysisAssessment assessment = engine.analyze(
                invalidModel,
                context()
        );

        assertEquals("validation", engine.id());
        assertEquals(AssessmentStatus.FAILED, assessment.status());
        assertFalse(assessment.findings().isEmpty());
    }

    private AnalysisContext context() {
        return new AnalysisContext(
                "QAIP-TEST-000003",
                "0.6",
                "EP-0003",
                "0.1",
                Instant.parse("2026-07-16T12:00:00Z")
        );
    }
}
