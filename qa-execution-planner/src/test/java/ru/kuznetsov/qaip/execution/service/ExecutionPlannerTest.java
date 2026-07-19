package ru.kuznetsov.qaip.execution.service;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.execution.model.ExecutionWave;
import ru.kuznetsov.qaip.findings.model.Finding;
import ru.kuznetsov.qaip.findings.model.FindingCode;
import ru.kuznetsov.qaip.findings.model.FindingSeverity;
import ru.kuznetsov.qaip.findings.model.FindingsReport;
import ru.kuznetsov.qaip.findings.model.FindingsSummary;
import ru.kuznetsov.qaip.roadmap.model.RemediationTask;
import ru.kuznetsov.qaip.roadmap.model.RemediationTaskStatus;
import ru.kuznetsov.qaip.roadmap.model.RemediationTaskType;
import ru.kuznetsov.qaip.roadmap.model.RoadmapReport;
import ru.kuznetsov.qaip.roadmap.model.RoadmapSummary;
import ru.kuznetsov.qaip.roadmap.service.RoadmapService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutionPlannerTest {

    private final ExecutionPlanner planner = new ExecutionPlanner();

    @Test
    void nullRoadmapShouldBeRejected() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> planner.plan(null)
        );
        assertEquals("roadmapReport must not be null", exception.getMessage());
    }

    @Test
    void emptyRoadmapShouldProduceSuccessfulEmptyPlan() {
        RoadmapReport roadmap = roadmap(List.of());

        var plan = planner.plan(roadmap);

        assertTrue(plan.planned());
        assertEquals("0.1", plan.schemaVersion());
        assertEquals(0, plan.summary().totalTasks());
        assertEquals(0, plan.summary().totalWaves());
        assertEquals(0, plan.summary().parallelizableTasks());
        assertEquals(0, plan.summary().sequentialTasks());
        assertEquals(0, plan.summary().maximumParallelism());
        assertTrue(plan.waves().isEmpty());
        assertSame(roadmap.summary(), plan.sourceRoadmapSummary());
    }

    @Test
    void singleTaskShouldProduceOneSequentialWave() {
        var plan = planner.plan(roadmap(List.of(task("A", List.of()))));

        assertWaves(plan.waves(), List.of(List.of("A")));
        assertEquals(1, plan.summary().totalTasks());
        assertEquals(1, plan.summary().totalWaves());
        assertEquals(0, plan.summary().parallelizableTasks());
        assertEquals(1, plan.summary().sequentialTasks());
        assertEquals(1, plan.summary().maximumParallelism());
    }

    @Test
    void independentDifferentTaskTypesShouldStayInOneOrderedWave() {
        var plan = planner.plan(roadmap(List.of(
                task("CHECK-Z", RemediationTaskType.CREATE_CHECK, "TEST-Z", List.of()),
                task("SCENARIO-B", RemediationTaskType.CREATE_SCENARIO, "BR-B", List.of()),
                task("TEST-A", RemediationTaskType.CREATE_TEST_IMPLEMENTATION, "SC-A", List.of()),
                task("SCENARIO-A", RemediationTaskType.CREATE_SCENARIO, "BR-A", List.of())
        )));

        assertWaves(plan.waves(), List.of(List.of(
                "SCENARIO-A", "SCENARIO-B", "TEST-A", "CHECK-Z"
        )));
        assertEquals(4, plan.summary().parallelizableTasks());
        assertEquals(0, plan.summary().sequentialTasks());
        assertEquals(4, plan.summary().maximumParallelism());
    }

    @Test
    void linearDependenciesShouldProduceSequentialWaves() {
        var plan = planner.plan(roadmap(List.of(
                task("C", List.of("B")),
                task("A", List.of()),
                task("B", List.of("A"))
        )));

        assertWaves(plan.waves(), List.of(
                List.of("A"), List.of("B"), List.of("C")
        ));
        assertEquals(3, plan.summary().sequentialTasks());
        assertEquals(0, plan.summary().parallelizableTasks());
        assertEquals(1, plan.summary().maximumParallelism());
    }

    @Test
    void diamondShouldUseEarliestValidParallelWave() {
        var plan = planner.plan(roadmap(List.of(
                task("D", List.of("B", "C")),
                task("C", List.of("A")),
                task("A", List.of()),
                task("B", List.of("A"))
        )));

        assertWaves(plan.waves(), List.of(
                List.of("A"), List.of("B", "C"), List.of("D")
        ));
        assertEquals(2, plan.summary().parallelizableTasks());
        assertEquals(2, plan.summary().sequentialTasks());
        assertEquals(2, plan.summary().maximumParallelism());
    }

    @Test
    void disconnectedComponentsShouldBePlannedConcurrently() {
        var plan = planner.plan(roadmap(List.of(
                task("Z", List.of("Y")),
                task("B", List.of("A")),
                task("X", List.of()),
                task("Y", List.of("X")),
                task("A", List.of())
        )));

        assertWaves(plan.waves(), List.of(
                List.of("A", "X"),
                List.of("B", "Y"),
                List.of("Z")
        ));
    }

    @Test
    void multipleDependenciesShouldEnterEarliestPossibleWave() {
        var plan = planner.plan(roadmap(List.of(
                task("D", List.of("A", "B")),
                task("C", List.of("A")),
                task("B", List.of()),
                task("A", List.of()),
                task("E", List.of("C"))
        )));

        assertWaves(plan.waves(), List.of(
                List.of("A", "B"),
                List.of("C", "D"),
                List.of("E")
        ));
        assertEquals(5, plan.summary().parallelizableTasks()
                + plan.summary().sequentialTasks());
    }

    @Test
    void shuffledTasksAndDependenciesShouldProduceEqualPlans() {
        RoadmapReport first = roadmap(List.of(
                task("A", List.of()),
                task("B", List.of()),
                task("C", List.of("A", "B"))
        ));
        RoadmapReport second = roadmap(List.of(
                task("C", List.of("B", "A")),
                task("B", List.of()),
                task("A", List.of())
        ));

        assertEquals(planner.plan(first), planner.plan(second));
        assertEquals(planner.plan(first), planner.plan(first));
    }

    @Test
    void everyTaskShouldAppearOnceAndAfterDependencies() {
        var plan = planner.plan(roadmap(List.of(
                task("A", List.of()),
                task("B", List.of("A")),
                task("C", List.of("A")),
                task("D", List.of("B", "C"))
        )));
        List<String> taskIds = plan.waves().stream()
                .flatMap(wave -> wave.taskIds().stream())
                .toList();

        assertEquals(List.of("A", "B", "C", "D"), taskIds);
        assertEquals(4, taskIds.stream().distinct().count());
        assertEquals(List.of(1, 2, 3), plan.waves().stream()
                .map(ExecutionWave::number).toList());
    }

    @Test
    void duplicateDependencyReferencesShouldActAsOneEdge() {
        var plan = planner.plan(roadmap(List.of(
                task("A", List.of()),
                task("B", List.of("A", "A"))
        )));

        assertWaves(plan.waves(), List.of(List.of("A"), List.of("B")));
    }

    @Test
    void missingDependencyShouldFailWithBothTaskIds() {
        UnknownTaskDependencyException exception = assertThrows(
                UnknownTaskDependencyException.class,
                () -> planner.plan(roadmap(List.of(
                        task("A", List.of("MISSING"))
                )))
        );

        assertTrue(exception.getMessage().contains("A"));
        assertTrue(exception.getMessage().contains("MISSING"));
    }

    @Test
    void selfDependencyShouldFailWithTaskId() {
        SelfDependentTaskException exception = assertThrows(
                SelfDependentTaskException.class,
                () -> planner.plan(roadmap(List.of(
                        task("SELF", List.of("SELF"))
                )))
        );

        assertTrue(exception.getMessage().contains("SELF"));
    }

    @Test
    void duplicateTaskIdShouldFailEvenForIdenticalEntries() {
        RemediationTask duplicate = task("DUPLICATE", List.of());
        DuplicateRoadmapTaskIdException exception = assertThrows(
                DuplicateRoadmapTaskIdException.class,
                () -> planner.plan(roadmap(List.of(duplicate, duplicate)))
        );

        assertTrue(exception.getMessage().contains("DUPLICATE"));
    }

    @Test
    void twoAndThreeTaskCyclesShouldFailWithoutPartialPlan() {
        CyclicTaskDependencyException twoTask = assertThrows(
                CyclicTaskDependencyException.class,
                () -> planner.plan(roadmap(List.of(
                        task("B", List.of("A")),
                        task("A", List.of("B"))
                )))
        );
        assertEquals(List.of("A", "B"), twoTask.unresolvedTaskIds());

        CyclicTaskDependencyException threeTask = assertThrows(
                CyclicTaskDependencyException.class,
                () -> planner.plan(roadmap(List.of(
                        task("C", List.of("B")),
                        task("A", List.of("C")),
                        task("B", List.of("A")),
                        task("FREE", List.of())
                )))
        );
        assertEquals(List.of("A", "B", "C"),
                threeTask.unresolvedTaskIds());
        assertTrue(threeTask.getMessage().contains("A, B, C"));
    }

    @Test
    void outputCollectionsShouldBeImmutableAndInputUnchanged() {
        List<RemediationTask> mutableTasks = new ArrayList<>();
        mutableTasks.add(task("A", List.of()));
        RoadmapReport roadmap = roadmap(mutableTasks);
        RoadmapReport before = roadmap(List.copyOf(roadmap.tasks()));

        var plan = planner.plan(roadmap);

        assertEquals(before, roadmap);
        assertThrows(UnsupportedOperationException.class,
                () -> plan.waves().add(plan.waves().getFirst()));
        assertThrows(UnsupportedOperationException.class,
                () -> plan.waves().getFirst().taskIds().add("B"));
    }

    @Test
    void realRoadmapOutputShouldRemainOneWaveWithoutInferredDependencies() {
        FindingsReport findings = new FindingsReport(
                true,
                "0.1",
                new FindingsSummary(3, 1, 2, 0),
                List.of(
                        finding(FindingCode.TEST_WITHOUT_CHECK,
                                FindingSeverity.MEDIUM, "TEST-001",
                                "TEST_IMPLEMENTATION"),
                        finding(FindingCode.BUSINESS_RULE_WITHOUT_SCENARIO,
                                FindingSeverity.HIGH, "BR-001",
                                "BUSINESS_RULE"),
                        finding(FindingCode.SCENARIO_WITHOUT_TEST,
                                FindingSeverity.MEDIUM, "SC-001",
                                "SCENARIO")
                ),
                null
        );
        RoadmapReport roadmap = new RoadmapService().plan(findings);

        var plan = planner.plan(roadmap);

        assertEquals(3, plan.summary().totalTasks());
        assertEquals(1, plan.summary().totalWaves());
        assertEquals(3, plan.summary().maximumParallelism());
        assertEquals(List.of(
                        "TASK-CREATE-SCENARIO-BR-001",
                        "TASK-CREATE-TEST-IMPLEMENTATION-SC-001",
                        "TASK-CREATE-CHECK-TEST-001"
                ), plan.waves().getFirst().taskIds());
    }

    private void assertWaves(
            List<ExecutionWave> actual,
            List<List<String>> expectedTaskIds
    ) {
        assertEquals(expectedTaskIds, actual.stream()
                .map(ExecutionWave::taskIds)
                .toList());
        assertEquals(expectedTaskIds.size(), actual.size());
        for (int index = 0; index < actual.size(); index++) {
            assertEquals(index + 1, actual.get(index).number());
        }
    }

    private RoadmapReport roadmap(List<RemediationTask> tasks) {
        int tasksWithDependencies = (int) tasks.stream()
                .filter(task -> !task.dependsOn().isEmpty())
                .count();
        return new RoadmapReport(
                true,
                "0.1",
                new RoadmapSummary(
                        tasks.size(),
                        tasks.size(),
                        tasksWithDependencies
                ),
                tasks,
                new FindingsSummary(0, 0, 0, 0)
        );
    }

    private RemediationTask task(String id, List<String> dependencies) {
        return task(
                id,
                RemediationTaskType.CREATE_SCENARIO,
                id,
                dependencies
        );
    }

    private RemediationTask task(
            String id,
            RemediationTaskType type,
            String targetNodeId,
            List<String> dependencies
    ) {
        FindingCode code = switch (type) {
            case CREATE_SCENARIO ->
                    FindingCode.BUSINESS_RULE_WITHOUT_SCENARIO;
            case CREATE_TEST_IMPLEMENTATION ->
                    FindingCode.SCENARIO_WITHOUT_TEST;
            case CREATE_CHECK -> FindingCode.TEST_WITHOUT_CHECK;
        };
        return new RemediationTask(
                id,
                type,
                RemediationTaskStatus.PLANNED,
                code,
                targetNodeId,
                "NODE",
                "Description",
                dependencies
        );
    }

    private Finding finding(
            FindingCode code,
            FindingSeverity severity,
            String nodeId,
            String nodeType
    ) {
        return new Finding(
                code,
                severity,
                nodeId,
                nodeType,
                "message",
                "recommendation"
        );
    }
}
