package ru.kuznetsov.qaip.core.engine;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.analysis.AnalysisAssessment;
import ru.kuznetsov.qaip.core.analysis.AnalysisContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnalysisEngineRegistryTest {

    @Test
    void shouldSortEnginesByOrderAndId() {
        AnalysisEngineRegistry registry =
                new AnalysisEngineRegistry(List.of(
                        engine("z-engine", 20),
                        engine("b-engine", 10),
                        engine("a-engine", 10)
                ));

        assertEquals(
                List.of(
                        "a-engine",
                        "b-engine",
                        "z-engine"
                ),
                registry.engines().stream()
                        .map(AnalysisEngine::id)
                        .toList()
        );
    }

    private AnalysisEngine engine(
            String id,
            int order
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
                return AnalysisAssessment.skipped(
                        id,
                        id,
                        "test"
                );
            }
        };
    }
}
