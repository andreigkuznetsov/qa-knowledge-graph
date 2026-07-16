package ru.kuznetsov.qaip.analysis.orchestration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.analysis.execution.AnalysisExecutor;
import ru.kuznetsov.qaip.core.analysis.AnalysisAssessment;
import ru.kuznetsov.qaip.core.analysis.AnalysisContext;
import ru.kuznetsov.qaip.core.analysis.AssessmentStatus;
import ru.kuznetsov.qaip.core.engine.AnalysisEngine;
import ru.kuznetsov.qaip.core.engine.AnalysisEngineRegistry;
import ru.kuznetsov.qaip.core.metadata.AnalysisIdGenerator;
import ru.kuznetsov.qaip.core.metadata.MetadataFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AnalysisOrchestratorTest {

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    @Test
    void shouldCreateMetadataAndExecuteRegisteredEngines()
            throws Exception {

        AnalysisEngine engine =
                new AnalysisEngine() {
                    @Override
                    public String id() {
                        return "validation";
                    }

                    @Override
                    public String name() {
                        return "Validation";
                    }

                    @Override
                    public AnalysisAssessment analyze(
                            JsonNode qaModel,
                            AnalysisContext context
                    ) {
                        assertEquals(
                                "QAIP-TEST-000001",
                                context.analysisId()
                        );

                        return new AnalysisAssessment(
                                id(),
                                name(),
                                AssessmentStatus.SUCCESS,
                                List.of(),
                                List.of(),
                                null
                        );
                    }
                };

        AnalysisIdGenerator idGenerator =
                () -> "QAIP-TEST-000001";

        MetadataFactory metadataFactory =
                new MetadataFactory(
                        "0.6",
                        "RC1-Build-02A-Part-02",
                        idGenerator
                );

        AnalysisOrchestrator orchestrator =
                new AnalysisOrchestrator(
                        new AnalysisExecutor(
                                new AnalysisEngineRegistry(
                                        List.of(engine)
                                )
                        ),
                        metadataFactory
                );

        UnifiedAnalysisResult result =
                orchestrator.analyze(
                        model(),
                        "0.1"
                );

        assertEquals(
                "0.6",
                result.metadata().release()
        );
        assertEquals(
                "RC1-Build-02A-Part-02",
                result.metadata().build()
        );
        assertEquals(
                "QAIP-TEST-000001",
                result.metadata().analysisId()
        );
        assertEquals(
                1,
                result.execution()
                        .engineResults()
                        .size()
        );
        assertNotNull(
                result.execution().totalDuration()
        );
    }

    private JsonNode model() throws Exception {
        return objectMapper.readTree(
                "{\"nodes\":[],\"relationships\":[]}"
        );
    }
}
