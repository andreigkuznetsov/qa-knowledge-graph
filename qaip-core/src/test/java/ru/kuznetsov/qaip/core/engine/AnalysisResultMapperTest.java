package ru.kuznetsov.qaip.core.engine;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.analysis.AnalysisAssessment;
import ru.kuznetsov.qaip.core.analysis.AssessmentStatus;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnalysisResultMapperTest {

    @Test
    void shouldMapEngineSpecificResultToAssessment() {
        AnalysisResultMapper<String> mapper =
                result -> new AnalysisAssessment(
                        "test-engine",
                        "Test Engine",
                        AssessmentStatus.SUCCESS,
                        List.of(),
                        List.of(),
                        Map.of("sourceResult", result)
                );

        AnalysisAssessment assessment =
                mapper.map("engine-result");

        assertEquals(
                "test-engine",
                assessment.engineId()
        );
        assertEquals(
                AssessmentStatus.SUCCESS,
                assessment.status()
        );
        assertEquals(
                "engine-result",
                assessment.data()
                        .get("sourceResult")
        );
    }
}
