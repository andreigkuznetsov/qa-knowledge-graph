package ru.kuznetsov.qaip.analysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.analysis.execution.AnalysisExecutor;
import ru.kuznetsov.qaip.analysis.orchestration.AnalysisOrchestrator;
import ru.kuznetsov.qaip.core.engine.AnalysisEngineRegistry;
import ru.kuznetsov.qaip.core.metadata.AnalysisIdGenerator;
import ru.kuznetsov.qaip.core.metadata.MetadataFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnifiedAnalysisServiceTest {

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    @Test
    void shouldDelegateModelToOrchestrator()
            throws Exception {

        AnalysisIdGenerator idGenerator =
                () -> "QAIP-TEST-000002";

        UnifiedAnalysisService service =
                new UnifiedAnalysisService(
                        new AnalysisOrchestrator(
                                new AnalysisExecutor(
                                        new AnalysisEngineRegistry(
                                                List.of()
                                        )
                                ),
                                new MetadataFactory(
                                        "0.6",
                                        "RC1-Build-02A-Part-02",
                                        idGenerator
                                )
                        ),
                        "0.1"
                );

        JsonNode model =
                objectMapper.readTree(
                        "{\"nodes\":[],\"relationships\":[]}"
                );

        var result = service.analyze(model);

        assertEquals(
                "QAIP-TEST-000002",
                result.metadata().analysisId()
        );
        assertEquals(
                "0.1",
                result.metadata().schemaVersion()
        );
        assertEquals(
                0,
                result.execution()
                        .engineResults()
                        .size()
        );
    }
}
