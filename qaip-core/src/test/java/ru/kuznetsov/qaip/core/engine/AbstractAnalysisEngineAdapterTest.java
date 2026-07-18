package ru.kuznetsov.qaip.core.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.analysis.AnalysisAssessment;
import ru.kuznetsov.qaip.core.analysis.AnalysisContext;
import ru.kuznetsov.qaip.core.analysis.AssessmentStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractAnalysisEngineAdapterTest {

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    @Test
    void shouldExecuteEngineAndMapResult()
            throws Exception {

        AtomicBoolean executed =
                new AtomicBoolean(false);

        AnalysisResultMapper<String> mapper =
                result -> new AnalysisAssessment(
                        "test-engine",
                        "Test Engine",
                        AssessmentStatus.SUCCESS,
                        List.of(),
                        List.of(),
                        Map.of("result", result)
                );

        AbstractAnalysisEngineAdapter<String> adapter =
                new AbstractAnalysisEngineAdapter<>(mapper) {
                    @Override
                    public String id() {
                        return "test-engine";
                    }

                    @Override
                    public String name() {
                        return "Test Engine";
                    }

                    @Override
                    protected String execute(
                            JsonNode qaModel,
                            AnalysisContext context
                    ) {
                        executed.set(true);
                        return qaModel.path("value").asText();
                    }
                };

        AnalysisAssessment assessment =
                adapter.analyze(
                        objectMapper.readTree(
                                "{\"value\":\"engine-result\"}"
                        ),
                        context()
                );

        assertTrue(executed.get());
        assertEquals(
                "engine-result",
                assessment.data().get("result")
        );
    }

    @Test
    void shouldRejectNullInputModel() {
        AbstractAnalysisEngineAdapter<String> adapter =
                adapterReturning("result");

        assertThrows(
                NullPointerException.class,
                () -> adapter.analyze(
                        null,
                        context()
                )
        );
    }

    @Test
    void shouldRejectNullContext()
            throws Exception {

        AbstractAnalysisEngineAdapter<String> adapter =
                adapterReturning("result");

        assertThrows(
                NullPointerException.class,
                () -> adapter.analyze(
                        objectMapper.readTree("{}"),
                        null
                )
        );
    }

    @Test
    void shouldRejectNullEngineResult()
            throws Exception {

        AbstractAnalysisEngineAdapter<String> adapter =
                adapterReturning(null);

        assertThrows(
                NullPointerException.class,
                () -> adapter.analyze(
                        objectMapper.readTree("{}"),
                        context()
                )
        );
    }

    private AbstractAnalysisEngineAdapter<String>
    adapterReturning(String result) {

        return new AbstractAnalysisEngineAdapter<>(
                value -> new AnalysisAssessment(
                        "test-engine",
                        "Test Engine",
                        AssessmentStatus.SUCCESS,
                        List.of(),
                        List.of(),
                        Map.of()
                )
        ) {
            @Override
            public String id() {
                return "test-engine";
            }

            @Override
            public String name() {
                return "Test Engine";
            }

            @Override
            protected String execute(
                    JsonNode qaModel,
                    AnalysisContext context
            ) {
                return result;
            }
        };
    }

    private AnalysisContext context() {
        return new AnalysisContext(
                "QAIP-TEST-000005",
                "0.6",
                "EP-0005-02",
                "0.1",
                Instant.parse(
                        "2026-07-16T12:00:00Z"
                )
        );
    }
}
