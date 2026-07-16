package ru.kuznetsov.qaip.coverage.analyzer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import ru.kuznetsov.qaip.coverage.model.CoverageProblemType;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckCoverageAnalyzerTest {

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    private final CheckCoverageAnalyzer analyzer =
            new CheckCoverageAnalyzer();

    @Test
    void shouldCalculatePartialCheckCoverage()
            throws Exception {

        JsonNode qaModel = readFixture(
                "check-coverage-partial.json"
        );

        var result = analyzer.analyze(qaModel);
        var metric = result.metrics().getFirst();

        assertEquals(2, metric.total());
        assertEquals(1, metric.covered());
        assertEquals(1, metric.uncovered());
        assertEquals(50.0, metric.percentage());

        assertEquals(1, result.problems().size());
        assertEquals(
                CoverageProblemType.MISSING_CHECK,
                result.problems().getFirst().type()
        );
        assertEquals(
                "TEST-002",
                result.problems().getFirst().objectId()
        );
        assertEquals(
                "/nodes/8",
                result.problems().getFirst().path()
        );
    }

    @Test
    void wrongTargetTypeShouldNotCoverTest()
            throws Exception {

        JsonNode qaModel = objectMapper.readTree("""
                {
                  "nodes": [
                    {
                      "id": "TEST-001",
                      "type": "TEST_IMPLEMENTATION",
                      "name": "Test"
                    },
                    {
                      "id": "SC-001",
                      "type": "SCENARIO",
                      "name": "Scenario"
                    }
                  ],
                  "relationships": [
                    {
                      "id": "REL-001",
                      "from": "TEST-001",
                      "type": "HAS_CHECK",
                      "to": "SC-001"
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
                                "TEST-001".equals(
                                        problem.objectId()
                                ))
        );
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
