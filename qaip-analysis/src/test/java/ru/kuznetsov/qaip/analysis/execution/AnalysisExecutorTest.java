package ru.kuznetsov.qaip.analysis.execution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.analysis.AnalysisAssessment;
import ru.kuznetsov.qaip.core.analysis.AnalysisContext;
import ru.kuznetsov.qaip.core.analysis.AssessmentStatus;
import ru.kuznetsov.qaip.core.engine.AnalysisEngine;
import ru.kuznetsov.qaip.core.engine.AnalysisEngineRegistry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AnalysisExecutorTest {

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    @Test
    void shouldExecuteEnginesInRegistryOrder()
            throws Exception {

        List<String> executionOrder =
                new ArrayList<>();

        AnalysisEngineRegistry registry =
                new AnalysisEngineRegistry(List.of(
                        engine("coverage", 20, executionOrder),
                        engine("validation", 10, executionOrder)
                ));

        AnalysisExecutionResult result =
                new AnalysisExecutor(registry).execute(
                        model(),
                        context()
                );

        assertEquals(
                List.of("validation", "coverage"),
                executionOrder
        );
        assertEquals(
                2,
                result.engineResults().size()
        );
        assertFalse(
                result.totalDuration().isNegative()
        );
    }

    @Test
    void unsupportedEngineShouldBeSkipped()
            throws Exception {

        AnalysisEngine unsupported =
                new AnalysisEngine() {
                    @Override
                    public String id() {
                        return "unsupported";
                    }

                    @Override
                    public String name() {
                        return "Unsupported";
                    }

                    @Override
                    public boolean supports(
                            JsonNode qaModel
                    ) {
                        return false;
                    }

                    @Override
                    public AnalysisAssessment analyze(
                            JsonNode qaModel,
                            AnalysisContext context
                    ) {
                        throw new AssertionError(
                                "analyze must not be called"
                        );
                    }
                };

        AnalysisExecutionResult result =
                new AnalysisExecutor(
                        new AnalysisEngineRegistry(
                                List.of(unsupported)
                        )
                ).execute(
                        model(),
                        context()
                );

        assertEquals(
                AssessmentStatus.SKIPPED,
                result.engineResults()
                        .getFirst()
                        .assessment()
                        .status()
        );
    }

    private AnalysisEngine engine(
            String id,
            int order,
            List<String> executionOrder
    ) {
        return new AnalysisEngine() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String name() {
                return id;
            }

            @Override
            public int order() {
                return order;
            }

            @Override
            public AnalysisAssessment analyze(
                    JsonNode qaModel,
                    AnalysisContext context
            ) {
                executionOrder.add(id);

                return new AnalysisAssessment(
                        id,
                        id,
                        AssessmentStatus.SUCCESS,
                        List.of(),
                        List.of(),
                        null
                );
            }
        };
    }

    private JsonNode model() throws Exception {
        return objectMapper.readTree(
                "{\"nodes\":[],\"relationships\":[]}"
        );
    }

    private AnalysisContext context() {
        return new AnalysisContext(
                "QAIP-TEST-000001",
                "0.6",
                "RC1-Build-02A-Part-01",
                "0.1",
                Instant.parse(
                        "2026-07-16T12:00:00Z"
                )
        );
    }
}
