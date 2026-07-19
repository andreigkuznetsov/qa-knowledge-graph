package ru.kuznetsov.qagraph.api;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;
import ru.kuznetsov.qagraph.validationcore.model.ValidationSummary;
import ru.kuznetsov.qaip.execution.model.ExecutionPlan;
import ru.kuznetsov.qaip.execution.model.ExecutionPlanSummary;
import ru.kuznetsov.qaip.execution.model.ExecutionWave;
import ru.kuznetsov.qaip.roadmap.model.RoadmapSummary;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutionPlanResponseMapperTest {

    private final ExecutionPlanResponseMapper mapper =
            new ExecutionPlanResponseMapper();

    @Test
    void shouldMapAllValuesWithoutReorderingOrRecalculating() {
        List<String> firstTaskIds = new ArrayList<>(List.of("TASK-Z", "TASK-A"));
        ExecutionPlan plan = new ExecutionPlan(
                true,
                "0.1",
                new ExecutionPlanSummary(99, 88, 77, 66, 55),
                List.of(
                        new ExecutionWave(7, firstTaskIds),
                        new ExecutionWave(3, List.of("TASK-C", "TASK-B"))
                ),
                new RoadmapSummary(12, 11, 10)
        );
        QaModelValidationResult validation = validation();

        var response = mapper.map("model-1", plan, validation);
        firstTaskIds.add("TASK-LATE");

        assertEquals("model-1", response.modelId());
        assertTrue(response.planned());
        assertEquals("0.1", response.schemaVersion());
        assertEquals(new ExecutionPlanSummaryResponse(99, 88, 77, 66, 55),
                response.summary());
        assertEquals(List.of(7, 3), response.waves().stream()
                .map(ExecutionWaveResponse::number).toList());
        assertEquals(List.of("TASK-Z", "TASK-A"),
                response.waves().getFirst().taskIds());
        assertEquals(List.of("TASK-C", "TASK-B"),
                response.waves().get(1).taskIds());
        assertEquals(new RoadmapSummaryResponse(12, 11, 10),
                response.sourceRoadmapSummary());
        assertSame(validation, response.validation());
        assertThrows(UnsupportedOperationException.class,
                () -> response.waves().add(response.waves().getFirst()));
        assertThrows(UnsupportedOperationException.class,
                () -> response.waves().getFirst().taskIds().add("TASK-X"));
        assertEquals(response, mapper.map("model-1", plan, validation));
    }

    @Test
    void shouldMapEmptyPlanWithoutInventingWave() {
        var response = mapper.map(
                "model-1",
                new ExecutionPlan(
                        true,
                        "0.1",
                        new ExecutionPlanSummary(0, 0, 0, 0, 0),
                        List.of(),
                        new RoadmapSummary(0, 0, 0)
                ),
                validation()
        );

        assertTrue(response.waves().isEmpty());
        assertEquals(0, response.summary().totalTasks());
        assertEquals(0, response.summary().totalWaves());
        assertEquals(0, response.sourceRoadmapSummary().totalTasks());
    }

    @Test
    void shouldPreserveEmptyTaskIdListWhenDomainProvidesIt() {
        var response = mapper.map(
                "model-1",
                new ExecutionPlan(
                        true,
                        "0.1",
                        new ExecutionPlanSummary(0, 1, 0, 0, 0),
                        List.of(new ExecutionWave(1, List.of())),
                        new RoadmapSummary(0, 0, 0)
                ),
                validation()
        );

        assertEquals(1, response.waves().getFirst().number());
        assertTrue(response.waves().getFirst().taskIds().isEmpty());
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
