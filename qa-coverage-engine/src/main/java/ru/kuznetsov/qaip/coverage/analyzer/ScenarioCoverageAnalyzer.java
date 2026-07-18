package ru.kuznetsov.qaip.coverage.analyzer;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import ru.kuznetsov.qaip.coverage.model.CoverageAnalysisResult;
import ru.kuznetsov.qaip.coverage.model.CoverageMetric;
import ru.kuznetsov.qaip.coverage.model.CoverageProblem;
import ru.kuznetsov.qaip.coverage.model.CoverageProblemType;
import ru.kuznetsov.qaip.coverage.model.CoverageSeverity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ScenarioCoverageAnalyzer implements CoverageAnalyzer {

    public static final String METRIC_CODE =
            "SCENARIO_TEST_COVERAGE";

    @Override
    public CoverageAnalysisResult analyze(JsonNode qaModel) {
        Map<String, NodeInfo> scenarios =
                indexScenarios(qaModel.path("nodes"));

        Set<String> coveredScenarioIds =
                findCoveredScenarioIds(
                        qaModel.path("nodes"),
                        qaModel.path("relationships")
                );

        List<CoverageProblem> problems = new ArrayList<>();

        for (NodeInfo scenario : scenarios.values()) {
            if (!coveredScenarioIds.contains(scenario.id())) {
                problems.add(new CoverageProblem(
                        CoverageProblemType
                                .MISSING_TEST_IMPLEMENTATION,
                        CoverageSeverity.WARNING,
                        scenario.id(),
                        "SCENARIO",
                        scenario.name(),
                        "Для BDD-сценария отсутствует тестовая реализация",
                        "SCENARIO покрыт, только если существует связь "
                                + "TEST_IMPLEMENTATION --VALIDATES--> SCENARIO.",
                        scenario.path()
                ));
            }
        }

        int total = scenarios.size();
        int covered = (int) scenarios.keySet().stream()
                .filter(coveredScenarioIds::contains)
                .count();
        int uncovered = total - covered;

        CoverageMetric metric = new CoverageMetric(
                METRIC_CODE,
                "Покрытие BDD-сценариев тестовыми реализациями",
                total,
                covered,
                uncovered,
                percentage(covered, total)
        );

        return new CoverageAnalysisResult(
                List.of(metric),
                List.copyOf(problems)
        );
    }

    private Map<String, NodeInfo> indexScenarios(JsonNode nodes) {
        Map<String, NodeInfo> result = new LinkedHashMap<>();

        for (int index = 0; index < nodes.size(); index++) {
            JsonNode node = nodes.get(index);

            if (!"SCENARIO".equals(text(node, "type"))) {
                continue;
            }

            String id = text(node, "id");

            result.put(
                    id,
                    new NodeInfo(
                            id,
                            text(node, "name"),
                            "/nodes/" + index
                    )
            );
        }

        return result;
    }

    private Set<String> findCoveredScenarioIds(
            JsonNode nodes,
            JsonNode relationships
    ) {
        Map<String, String> nodeTypes = new HashMap<>();

        for (JsonNode node : nodes) {
            nodeTypes.put(
                    text(node, "id"),
                    text(node, "type")
            );
        }

        Set<String> covered = new HashSet<>();

        for (JsonNode relationship : relationships) {
            if (!"VALIDATES".equals(
                    text(relationship, "type")
            )) {
                continue;
            }

            String from = text(relationship, "from");
            String to = text(relationship, "to");

            if ("TEST_IMPLEMENTATION".equals(
                    nodeTypes.get(from)
            ) && "SCENARIO".equals(nodeTypes.get(to))) {
                covered.add(to);
            }
        }

        return covered;
    }

    private double percentage(int covered, int total) {
        if (total == 0) {
            return 100.0;
        }

        return Math.round(covered * 10000.0 / total) / 100.0;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);

        return value == null || value.isNull()
                ? null
                : value.asText();
    }

    private record NodeInfo(
            String id,
            String name,
            String path
    ) {
    }
}
