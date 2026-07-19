package ru.kuznetsov.qagraph.api;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;
import ru.kuznetsov.qagraph.validationcore.model.ValidationSummary;
import ru.kuznetsov.qaip.findings.model.FindingCode;
import ru.kuznetsov.qaip.findings.model.FindingsSummary;
import ru.kuznetsov.qaip.roadmap.model.RemediationTask;
import ru.kuznetsov.qaip.roadmap.model.RemediationTaskStatus;
import ru.kuznetsov.qaip.roadmap.model.RemediationTaskType;
import ru.kuznetsov.qaip.roadmap.model.RoadmapReport;
import ru.kuznetsov.qaip.roadmap.model.RoadmapSummary;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoadmapResponseMapperTest {

    private final RoadmapResponseMapper mapper = new RoadmapResponseMapper();

    @Test
    void shouldMapEveryFieldAndPreserveTaskOrder() {
        List<String> dependencies = new ArrayList<>(List.of("TASK-A"));
        RemediationTask first = task(
                "TASK-CREATE-SCENARIO-BR-001",
                RemediationTaskType.CREATE_SCENARIO,
                FindingCode.BUSINESS_RULE_WITHOUT_SCENARIO,
                "BR-001",
                "BUSINESS_RULE",
                dependencies
        );
        RemediationTask second = task(
                "TASK-CREATE-CHECK-TEST-001",
                RemediationTaskType.CREATE_CHECK,
                FindingCode.TEST_WITHOUT_CHECK,
                "TEST-001",
                "TEST_IMPLEMENTATION",
                List.of()
        );
        FindingsSummary findingsSummary = new FindingsSummary(2, 1, 1, 0);
        QaModelValidationResult validation = validation();
        RoadmapReport report = new RoadmapReport(
                true,
                "0.1",
                new RoadmapSummary(2, 2, 1),
                List.of(first, second),
                findingsSummary
        );

        var response = mapper.map("model-1", report, validation);
        dependencies.add("TASK-B");

        assertEquals("model-1", response.modelId());
        assertTrue(response.planned());
        assertEquals("0.1", response.schemaVersion());
        assertEquals(2, response.summary().totalTasks());
        assertEquals(2, response.summary().plannedTasks());
        assertEquals(1, response.summary().tasksWithDependencies());
        assertEquals(List.of(
                        "TASK-CREATE-SCENARIO-BR-001",
                        "TASK-CREATE-CHECK-TEST-001"
                ), response.tasks().stream()
                        .map(RemediationTaskResponse::id)
                        .toList());
        var mapped = response.tasks().getFirst();
        assertEquals("CREATE_SCENARIO", mapped.type());
        assertEquals("PLANNED", mapped.status());
        assertEquals("BUSINESS_RULE_WITHOUT_SCENARIO",
                mapped.sourceFindingCode());
        assertEquals("BR-001", mapped.targetNodeId());
        assertEquals("BUSINESS_RULE", mapped.targetNodeType());
        assertEquals("Description for BR-001", mapped.description());
        assertEquals(List.of("TASK-A"), mapped.dependsOn());
        assertEquals(2, response.sourceFindingsSummary().total());
        assertEquals(1, response.sourceFindingsSummary().high());
        assertEquals(validation, response.validation());
        assertThrows(UnsupportedOperationException.class,
                () -> response.tasks().add(mapped));
        assertThrows(UnsupportedOperationException.class,
                () -> mapped.dependsOn().add("TASK-C"));
    }

    @Test
    void shouldMapEmptyTaskList() {
        var response = mapper.map(
                "model-1",
                new RoadmapReport(
                        true,
                        "0.1",
                        new RoadmapSummary(0, 0, 0),
                        List.of(),
                        new FindingsSummary(0, 0, 0, 0)
                ),
                validation()
        );

        assertTrue(response.tasks().isEmpty());
        assertEquals(0, response.summary().totalTasks());
    }

    private RemediationTask task(
            String id,
            RemediationTaskType type,
            FindingCode code,
            String nodeId,
            String nodeType,
            List<String> dependencies
    ) {
        return new RemediationTask(
                id,
                type,
                RemediationTaskStatus.PLANNED,
                code,
                nodeId,
                nodeType,
                "Description for " + nodeId,
                dependencies
        );
    }

    private QaModelValidationResult validation() {
        return new QaModelValidationResult(
                true,
                "0.1",
                new ValidationSummary(0, 2, 2),
                List.of()
        );
    }
}
