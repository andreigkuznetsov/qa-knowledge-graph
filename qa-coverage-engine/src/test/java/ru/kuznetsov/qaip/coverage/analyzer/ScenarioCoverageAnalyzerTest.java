package ru.kuznetsov.qaip.coverage.analyzer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import ru.kuznetsov.qaip.coverage.model.CoverageProblemType;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioCoverageAnalyzerTest {

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    private final ScenarioCoverageAnalyzer analyzer =
            new ScenarioCoverageAnalyzer();

    @Test
    void shouldCalculatePartialScenarioCoverage()
            throws Exception {

        JsonNode qaModel = readFixture(
                "scenario-coverage-partial.json"
        );

        var result = analyzer.analyze(qaModel);
        var metric = result.metrics().getFirst();

        assertEquals(2, metric.total());
        assertEquals(1, metric.covered());
        assertEquals(1, metric.uncovered());
        assertEquals(50.0, metric.percentage());

        assertEquals(1, result.problems().size());
        assertEquals(
                CoverageProblemType
                        .MISSING_TEST_IMPLEMENTATION,
                result.problems().getFirst().type()
        );
        assertEquals(
                "SC-002",
                result.problems().getFirst().objectId()
        );
        assertEquals(
                "/nodes/6",
                result.problems().getFirst().path()
        );
    }

    @Test
    void wrongDirectionShouldNotCoverScenario()
            throws Exception {

        JsonNode qaModel = objectMapper.readTree("""
                {
                  "nodes": [
                    {
                      "id": "SC-001",
                      "type": "SCENARIO",
                      "name": "Scenario"
                    },
                    {
                      "id": "TEST-001",
                      "type": "TEST_IMPLEMENTATION",
                      "name": "Test"
                    }
                  ],
                  "relationships": [
                    {
                      "id": "REL-001",
                      "from": "SC-001",
                      "type": "VALIDATES",
                      "to": "TEST-001"
                    }
                  ]
                }
                """);

        var result = analyzer.analyze(qaModel);

        assertEquals(
                0,
                result.metrics().getFirst().covered()
        );
        assertTrue(
                result.problems().stream()
                        .anyMatch(problem ->
                                "SC-001".equals(
                                        problem.objectId()
                                ))
        );
    }

    @Test
    void shouldPreserveInputOrderForMultipleScenarioFindings()
            throws Exception {

        JsonNode qaModel = objectMapper.readTree("""
                {
                  "nodes": [
                    {"id":"SC-300","type":"SCENARIO","name":"Third"},
                    {"id":"SC-100","type":"SCENARIO","name":"First"},
                    {"id":"SC-200","type":"SCENARIO","name":"Second"}
                  ],
                  "relationships": []
                }
                """);

        for (int execution = 0; execution < 10; execution++) {
            var result = analyzer.analyze(qaModel);

            assertEquals(3, result.problems().size());
            assertEquals(
                    List.of(
                            CoverageProblemType.MISSING_TEST_IMPLEMENTATION,
                            CoverageProblemType.MISSING_TEST_IMPLEMENTATION,
                            CoverageProblemType.MISSING_TEST_IMPLEMENTATION
                    ),
                    result.problems().stream()
                            .map(problem -> problem.type())
                            .toList()
            );
            assertEquals(
                    List.of("SC-300", "SC-100", "SC-200"),
                    result.problems().stream()
                            .map(problem -> problem.objectId())
                            .toList()
            );
            assertEquals(
                    List.of("/nodes/0", "/nodes/1", "/nodes/2"),
                    result.problems().stream()
                            .map(problem -> problem.path())
                            .toList()
            );

            var metric = result.metrics().getFirst();
            assertEquals(3, metric.total());
            assertEquals(0, metric.covered());
            assertEquals(3, metric.uncovered());
            assertEquals(0.0, metric.percentage());
        }
    }

    private JsonNode readFixture(String name)
            throws Exception {

        try (InputStream inputStream =
                     new ClassPathResource(name)
                             .getInputStream()) {

            return objectMapper.readTree(inputStream);
        }
    }
}
