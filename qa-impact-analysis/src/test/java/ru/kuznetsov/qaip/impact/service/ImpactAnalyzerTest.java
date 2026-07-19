package ru.kuznetsov.qaip.impact.service;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.model.NodeType;
import ru.kuznetsov.qagraph.model.RelationshipType;
import ru.kuznetsov.qaip.execution.model.ExecutionPlan;
import ru.kuznetsov.qaip.execution.model.ExecutionPlanSummary;
import ru.kuznetsov.qaip.execution.model.ExecutionWave;
import ru.kuznetsov.qaip.execution.service.ExecutionPlanner;
import ru.kuznetsov.qaip.findings.model.Finding;
import ru.kuznetsov.qaip.findings.model.FindingCode;
import ru.kuznetsov.qaip.findings.model.FindingSeverity;
import ru.kuznetsov.qaip.findings.model.FindingsReport;
import ru.kuznetsov.qaip.findings.model.FindingsSummary;
import ru.kuznetsov.qaip.impact.mapping.RemediationImpactCatalog;
import ru.kuznetsov.qaip.impact.model.ImpactChangeType;
import ru.kuznetsov.qaip.impact.model.ImpactReport;
import ru.kuznetsov.qaip.impact.model.ImpactSummary;
import ru.kuznetsov.qaip.impact.model.RelationEndpointRole;
import ru.kuznetsov.qaip.impact.model.ResolutionExpectation;
import ru.kuznetsov.qaip.impact.model.TaskImpact;
import ru.kuznetsov.qaip.roadmap.model.RemediationTask;
import ru.kuznetsov.qaip.roadmap.model.RemediationTaskStatus;
import ru.kuznetsov.qaip.roadmap.model.RemediationTaskType;
import ru.kuznetsov.qaip.roadmap.model.RoadmapReport;
import ru.kuznetsov.qaip.roadmap.model.RoadmapSummary;
import ru.kuznetsov.qaip.roadmap.service.RoadmapService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImpactAnalyzerTest {

    private final ImpactAnalyzer analyzer = new ImpactAnalyzer();

    @Test
    void emptyRoadmapAndPlanShouldProduceAnalyzedEmptyReport() {
        RoadmapReport roadmap = roadmap(List.of());

        var report = analyzer.analyze(roadmap, plan(roadmap, List.of()));

        assertTrue(report.analyzed());
        assertEquals("0.1", report.schemaVersion());
        assertTrue(report.taskImpacts().isEmpty());
        assertEquals(0, report.summary().totalTasks());
        assertEquals(0, report.summary().addressedFindingInstances());
        assertEquals(0, report.summary().businessRulesAffected());
        assertEquals(0, report.summary().scenariosAffected());
        assertEquals(0, report.summary().testImplementationsAffected());
        assertEquals(0, report.summary().wavesAffected());
        assertEquals(0, report.summary().tasksWithDependencies());
        assertEquals(0, report.summary().parallelizableTasks());
    }

    @Test
    void createScenarioShouldDescribeConditionalScenarioImpact() {
        TaskImpact impact = analyzeSingle(task(
                "TASK-SCENARIO",
                RemediationTaskType.CREATE_SCENARIO,
                FindingCode.BUSINESS_RULE_WITHOUT_SCENARIO,
                "BR-001",
                "BUSINESS_RULE",
                List.of()
        ));

        assertImpact(
                impact,
                "TASK-SCENARIO",
                RemediationTaskType.CREATE_SCENARIO,
                "BR-001",
                FindingCode.BUSINESS_RULE_WITHOUT_SCENARIO,
                NodeType.BUSINESS_RULE,
                NodeType.SCENARIO,
                RelationshipType.COVERS,
                RelationEndpointRole.TARGET
        );
    }

    @Test
    void createTestImplementationShouldDescribeConditionalTestImpact() {
        TaskImpact impact = analyzeSingle(task(
                "TASK-TEST",
                RemediationTaskType.CREATE_TEST_IMPLEMENTATION,
                FindingCode.SCENARIO_WITHOUT_TEST,
                "SC-001",
                "SCENARIO",
                List.of()
        ));

        assertImpact(
                impact,
                "TASK-TEST",
                RemediationTaskType.CREATE_TEST_IMPLEMENTATION,
                "SC-001",
                FindingCode.SCENARIO_WITHOUT_TEST,
                NodeType.SCENARIO,
                NodeType.TEST_IMPLEMENTATION,
                RelationshipType.VALIDATES,
                RelationEndpointRole.TARGET
        );
    }

    @Test
    void createCheckShouldDescribeConditionalCheckImpact() {
        TaskImpact impact = analyzeSingle(task(
                "TASK-CHECK",
                RemediationTaskType.CREATE_CHECK,
                FindingCode.TEST_WITHOUT_CHECK,
                "TC-001",
                "TEST_IMPLEMENTATION",
                List.of()
        ));

        assertImpact(
                impact,
                "TASK-CHECK",
                RemediationTaskType.CREATE_CHECK,
                "TC-001",
                FindingCode.TEST_WITHOUT_CHECK,
                NodeType.TEST_IMPLEMENTATION,
                NodeType.CHECK,
                RelationshipType.HAS_CHECK,
                RelationEndpointRole.SOURCE
        );
    }

    @Test
    void allSupportedTasksShouldPreserveOneWaveOrderAndRemainIndependent() {
        List<RemediationTask> tasks = supportedTasks();
        RoadmapReport roadmap = roadmap(tasks);
        ExecutionPlan executionPlan = plan(roadmap, List.of(
                new ExecutionWave(1, List.of(
                        "TASK-TEST", "TASK-CHECK", "TASK-SCENARIO"
                ))
        ));

        var report = analyzer.analyze(roadmap, executionPlan);

        assertEquals(List.of("TASK-TEST", "TASK-CHECK", "TASK-SCENARIO"),
                report.taskImpacts().stream().map(TaskImpact::taskId).toList());
        assertTrue(report.taskImpacts().stream()
                .allMatch(impact -> impact.executionWave() == 1));
        assertTrue(report.taskImpacts().stream()
                .allMatch(impact -> impact.dependsOn().isEmpty()));
        assertEquals(3, report.summary().totalTasks());
        assertEquals(3, report.summary().addressedFindingInstances());
        assertEquals(1, report.summary().businessRulesAffected());
        assertEquals(1, report.summary().scenariosAffected());
        assertEquals(1, report.summary().testImplementationsAffected());
        assertEquals(1, report.summary().wavesAffected());
        assertEquals(3, report.summary().parallelizableTasks());
    }

    @Test
    void multipleWavesShouldPreserveAssignmentsAndExplicitDependencies() {
        RemediationTask scenario = supportedTasks().get(2);
        RemediationTask test = task(
                "TASK-TEST",
                RemediationTaskType.CREATE_TEST_IMPLEMENTATION,
                FindingCode.SCENARIO_WITHOUT_TEST,
                "SC-001",
                "SCENARIO",
                List.of("TASK-SCENARIO")
        );
        RemediationTask check = task(
                "TASK-CHECK",
                RemediationTaskType.CREATE_CHECK,
                FindingCode.TEST_WITHOUT_CHECK,
                "TC-001",
                "TEST_IMPLEMENTATION",
                List.of("TASK-TEST")
        );
        RoadmapReport roadmap = roadmap(List.of(check, scenario, test));
        ExecutionPlan executionPlan = plan(roadmap, List.of(
                new ExecutionWave(1, List.of("TASK-SCENARIO")),
                new ExecutionWave(2, List.of("TASK-TEST")),
                new ExecutionWave(3, List.of("TASK-CHECK"))
        ));

        var report = analyzer.analyze(roadmap, executionPlan);

        assertEquals(List.of(1, 2, 3), report.taskImpacts().stream()
                .map(TaskImpact::executionWave).toList());
        assertEquals(List.of(), report.taskImpacts().get(0).dependsOn());
        assertEquals(List.of("TASK-SCENARIO"),
                report.taskImpacts().get(1).dependsOn());
        assertEquals(List.of("TASK-TEST"),
                report.taskImpacts().get(2).dependsOn());
        assertEquals(3, report.summary().wavesAffected());
        assertEquals(2, report.summary().tasksWithDependencies());
        assertEquals(0, report.summary().parallelizableTasks());
    }

    @Test
    void distinctAffectedCountsShouldCountNodeIdsNotTasks() {
        RemediationTask first = task(
                "TASK-A",
                RemediationTaskType.CREATE_SCENARIO,
                FindingCode.BUSINESS_RULE_WITHOUT_SCENARIO,
                "BR-001",
                "BUSINESS_RULE",
                List.of()
        );
        RemediationTask second = task(
                "TASK-B",
                RemediationTaskType.CREATE_SCENARIO,
                FindingCode.BUSINESS_RULE_WITHOUT_SCENARIO,
                "BR-001",
                "BUSINESS_RULE",
                List.of()
        );
        RoadmapReport roadmap = roadmap(List.of(first, second));

        var report = analyzer.analyze(roadmap, plan(roadmap, List.of(
                new ExecutionWave(1, List.of("TASK-A", "TASK-B"))
        )));

        assertEquals(2, report.summary().totalTasks());
        assertEquals(2, report.summary().addressedFindingInstances());
        assertEquals(1, report.summary().businessRulesAffected());
    }

    @Test
    void unknownExecutionTaskShouldFailExplicitly() {
        RoadmapReport roadmap = roadmap(List.of());

        assertFailure(
                ImpactAnalysisErrorCode.UNKNOWN_EXECUTION_TASK,
                "UNKNOWN",
                () -> analyzer.analyze(roadmap, plan(roadmap, List.of(
                        new ExecutionWave(1, List.of("UNKNOWN"))
                )))
        );
    }

    @Test
    void missingExecutionAssignmentShouldFailExplicitly() {
        RoadmapReport roadmap = roadmap(List.of(supportedTasks().getFirst()));

        assertFailure(
                ImpactAnalysisErrorCode.ROADMAP_TASK_NOT_SCHEDULED,
                "TASK-TEST",
                () -> analyzer.analyze(roadmap, plan(roadmap, List.of()))
        );
    }

    @Test
    void duplicateExecutionAssignmentInOneWaveShouldFailExplicitly() {
        RoadmapReport roadmap = roadmap(List.of(supportedTasks().getFirst()));

        assertFailure(
                ImpactAnalysisErrorCode.DUPLICATE_EXECUTION_TASK,
                "TASK-TEST",
                () -> analyzer.analyze(roadmap, plan(roadmap, List.of(
                        new ExecutionWave(1, List.of("TASK-TEST", "TASK-TEST"))
                )))
        );
    }

    @Test
    void duplicateExecutionAssignmentAcrossWavesShouldFailExplicitly() {
        RoadmapReport roadmap = roadmap(List.of(supportedTasks().getFirst()));

        assertFailure(
                ImpactAnalysisErrorCode.DUPLICATE_EXECUTION_TASK,
                "TASK-TEST",
                () -> analyzer.analyze(roadmap, plan(roadmap, List.of(
                        new ExecutionWave(1, List.of("TASK-TEST")),
                        new ExecutionWave(2, List.of("TASK-TEST"))
                )))
        );
    }

    @Test
    void duplicateRoadmapTaskIdShouldFailExplicitly() {
        RemediationTask duplicate = supportedTasks().getFirst();
        RoadmapReport roadmap = new RoadmapReport(
                true,
                "0.1",
                new RoadmapSummary(2, 2, 0),
                List.of(duplicate, duplicate),
                new FindingsSummary(2, 0, 2, 0)
        );

        assertFailure(
                ImpactAnalysisErrorCode.DUPLICATE_ROADMAP_TASK_ID,
                duplicate.id(),
                () -> analyzer.analyze(roadmap, plan(roadmap, List.of()))
        );
    }

    @Test
    void invalidWaveNumberShouldFailExplicitly() {
        RoadmapReport roadmap = roadmap(List.of(supportedTasks().getFirst()));

        assertFailure(
                ImpactAnalysisErrorCode.INVALID_EXECUTION_WAVE,
                "must be 1",
                () -> analyzer.analyze(roadmap, plan(roadmap, List.of(
                        new ExecutionWave(2, List.of("TASK-TEST"))
                )))
        );
    }

    @Test
    void emptyExecutionWaveShouldFailExplicitly() {
        RoadmapReport roadmap = roadmap(List.of());

        assertFailure(
                ImpactAnalysisErrorCode.INVALID_EXECUTION_WAVE,
                "at least one task",
                () -> analyzer.analyze(roadmap, plan(roadmap, List.of(
                        new ExecutionWave(1, List.of())
                )))
        );
    }

    @Test
    void inconsistentRoadmapSummaryAndTaskSemanticsShouldFail() {
        RoadmapReport roadmap = roadmap(List.of(supportedTasks().getFirst()));
        RoadmapReport wrongRoadmapSummary = new RoadmapReport(
                true,
                "0.1",
                new RoadmapSummary(99, 99, 99),
                roadmap.tasks(),
                roadmap.sourceFindingsSummary()
        );
        assertFailure(
                ImpactAnalysisErrorCode.INCONSISTENT_INPUT,
                "Roadmap summary",
                () -> analyzer.analyze(
                        wrongRoadmapSummary,
                        plan(wrongRoadmapSummary, List.of())
                )
        );

        RemediationTask wrongFinding = task(
                "TASK-WRONG",
                RemediationTaskType.CREATE_SCENARIO,
                FindingCode.TEST_WITHOUT_CHECK,
                "BR-001",
                "BUSINESS_RULE",
                List.of()
        );
        RoadmapReport wrongTaskRoadmap = roadmap(List.of(wrongFinding));
        assertFailure(
                ImpactAnalysisErrorCode.INCONSISTENT_INPUT,
                "TASK-WRONG",
                () -> analyzer.analyze(
                        wrongTaskRoadmap,
                        plan(wrongTaskRoadmap, List.of(
                                new ExecutionWave(1, List.of("TASK-WRONG"))
                        ))
                )
        );
    }

    @Test
    void differentRoadmapAndExecutionSchemaVersionsShouldBeCompatible() {
        RoadmapReport roadmap = roadmap(List.of(supportedTasks().getFirst()));
        ExecutionPlan executionPlan = new ExecutionPlan(
                true,
                "execution-plan-2.0",
                new ExecutionPlanSummary(1, 1, 0, 1, 1),
                List.of(new ExecutionWave(1, List.of("TASK-TEST"))),
                roadmap.summary()
        );

        var report = analyzer.analyze(roadmap, executionPlan);

        assertTrue(report.analyzed());
        assertEquals("0.1", report.schemaVersion());
        assertEquals(List.of("TASK-TEST"), report.taskImpacts().stream()
                .map(TaskImpact::taskId).toList());
    }

    @Test
    void executionPlannerShouldRemainOwnerOfSummarySemantics() {
        RoadmapReport roadmap = roadmap(List.of(supportedTasks().getFirst()));
        ExecutionPlan executionPlan = new ExecutionPlan(
                true,
                "0.1",
                new ExecutionPlanSummary(99, 98, 42, 97, 96),
                List.of(new ExecutionWave(1, List.of("TASK-TEST"))),
                roadmap.summary()
        );

        var report = analyzer.analyze(roadmap, executionPlan);

        assertEquals(42, report.summary().parallelizableTasks());
        assertEquals(1, report.summary().totalTasks());
    }

    @Test
    void nullInputsShouldBeRejectedClearly() {
        NullPointerException roadmap = assertThrows(
                NullPointerException.class,
                () -> analyzer.analyze(null, null)
        );
        assertEquals("roadmapReport must not be null", roadmap.getMessage());

        RoadmapReport validRoadmap = roadmap(List.of());
        NullPointerException plan = assertThrows(
                NullPointerException.class,
                () -> analyzer.analyze(validRoadmap, null)
        );
        assertEquals("executionPlan must not be null", plan.getMessage());
    }

    @Test
    void nullDomainCollectionsShouldBeRejectedWithExplicitMessages() {
        TaskImpact impact = analyzeSingle(supportedTasks().getFirst());

        NullPointerException dependencies = assertThrows(
                NullPointerException.class,
                () -> new TaskImpact(
                        impact.taskId(),
                        impact.taskType(),
                        impact.targetNodeId(),
                        impact.sourceFindingCode(),
                        impact.structuralGap(),
                        impact.expectedChange(),
                        impact.executionWave(),
                        null
                )
        );
        assertEquals("dependsOn must not be null", dependencies.getMessage());

        NullPointerException taskImpacts = assertThrows(
                NullPointerException.class,
                () -> new ImpactReport(
                        true,
                        "0.1",
                        new ImpactSummary(0, 0, 0, 0, 0, 0, 0, 0),
                        null
                )
        );
        assertEquals("taskImpacts must not be null", taskImpacts.getMessage());
    }

    @Test
    void catalogShouldCoverEveryCurrentRoadmapTaskTypeExactlyOnce() {
        assertEquals(
                Arrays.asList(RemediationTaskType.values()),
                RemediationImpactCatalog.definitions().keySet().stream()
                        .sorted().toList()
        );
        assertEquals(RemediationTaskType.values().length,
                RemediationImpactCatalog.definitions().size());
        assertThrows(UnsupportedOperationException.class,
                () -> RemediationImpactCatalog.definitions().clear());
    }

    @Test
    void outputsAndInputsShouldRemainImmutableAndUnchanged() {
        List<String> dependencies = new ArrayList<>(List.of("TASK-BASE"));
        RemediationTask task = task(
                "TASK-TEST",
                RemediationTaskType.CREATE_TEST_IMPLEMENTATION,
                FindingCode.SCENARIO_WITHOUT_TEST,
                "SC-001",
                "SCENARIO",
                dependencies
        );
        RemediationTask base = task(
                "TASK-BASE",
                RemediationTaskType.CREATE_SCENARIO,
                FindingCode.BUSINESS_RULE_WITHOUT_SCENARIO,
                "BR-001",
                "BUSINESS_RULE",
                List.of()
        );
        List<RemediationTask> taskInput = new ArrayList<>(List.of(base, task));
        RoadmapReport roadmap = roadmap(taskInput);
        List<String> waveTwoIds = new ArrayList<>(List.of("TASK-TEST"));
        ExecutionPlan plan = plan(roadmap, List.of(
                new ExecutionWave(1, List.of("TASK-BASE")),
                new ExecutionWave(2, waveTwoIds)
        ));
        RoadmapReport roadmapBefore = roadmap(taskInput);
        ExecutionPlan planBefore = plan(roadmapBefore, plan.waves());

        var report = analyzer.analyze(roadmap, plan);
        dependencies.add("LATE");
        taskInput.clear();
        waveTwoIds.add("LATE");

        assertEquals(roadmapBefore, roadmap);
        assertEquals(planBefore, plan);
        assertEquals(List.of("TASK-BASE"),
                report.taskImpacts().get(1).dependsOn());
        assertThrows(UnsupportedOperationException.class,
                () -> report.taskImpacts().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> report.taskImpacts().get(1).dependsOn().clear());
    }

    @Test
    void repeatedAnalysisShouldBeStructurallyIdentical() {
        RoadmapReport roadmap = roadmap(supportedTasks());
        ExecutionPlan plan = plan(roadmap, List.of(
                new ExecutionWave(1, List.of(
                        "TASK-SCENARIO", "TASK-TEST", "TASK-CHECK"
                ))
        ));

        assertEquals(
                analyzer.analyze(roadmap, plan),
                analyzer.analyze(roadmap, plan)
        );
    }

    @Test
    void realFindingsRoadmapPlannerPipelineShouldProduceThreeImpacts() {
        FindingsReport findings = new FindingsReport(
                true,
                "0.1",
                new FindingsSummary(3, 1, 2, 0),
                List.of(
                        finding(FindingCode.TEST_WITHOUT_CHECK,
                                FindingSeverity.MEDIUM,
                                "TC-001", "TEST_IMPLEMENTATION"),
                        finding(FindingCode.BUSINESS_RULE_WITHOUT_SCENARIO,
                                FindingSeverity.HIGH,
                                "BR-001", "BUSINESS_RULE"),
                        finding(FindingCode.SCENARIO_WITHOUT_TEST,
                                FindingSeverity.MEDIUM,
                                "SC-001", "SCENARIO")
                ),
                null
        );
        RoadmapReport roadmap = new RoadmapService().plan(findings);
        ExecutionPlan plan = new ExecutionPlanner().plan(roadmap);

        var report = analyzer.analyze(roadmap, plan);

        assertEquals(3, roadmap.tasks().size());
        assertEquals(1, plan.waves().size());
        assertEquals(3, report.taskImpacts().size());
        assertEquals(3, report.summary().parallelizableTasks());
        assertEquals(0, report.summary().tasksWithDependencies());
        assertTrue(report.taskImpacts().stream()
                .allMatch(impact -> impact.dependsOn().isEmpty()));
    }

    private TaskImpact analyzeSingle(RemediationTask task) {
        RoadmapReport roadmap = roadmap(List.of(task));
        var report = analyzer.analyze(roadmap, plan(roadmap, List.of(
                new ExecutionWave(1, List.of(task.id()))
        )));
        assertEquals(1, report.summary().totalTasks());
        assertEquals(1, report.summary().addressedFindingInstances());
        assertEquals(1, report.summary().wavesAffected());
        assertEquals(0, report.summary().tasksWithDependencies());
        assertEquals(0, report.summary().parallelizableTasks());
        return report.taskImpacts().getFirst();
    }

    private void assertImpact(
            TaskImpact impact,
            String taskId,
            RemediationTaskType taskType,
            String targetNodeId,
            FindingCode findingCode,
            NodeType affectedNodeType,
            NodeType requiredNodeType,
            RelationshipType relationshipType,
            RelationEndpointRole existingNodeRole
    ) {
        assertEquals(taskId, impact.taskId());
        assertEquals(taskType, impact.taskType());
        assertEquals(targetNodeId, impact.targetNodeId());
        assertEquals(findingCode, impact.sourceFindingCode());
        assertEquals(affectedNodeType,
                impact.structuralGap().affectedNodeType());
        assertEquals(requiredNodeType,
                impact.structuralGap().requiredNodeType());
        assertEquals(relationshipType,
                impact.structuralGap().requiredRelationshipType());
        assertEquals(existingNodeRole,
                impact.structuralGap().affectedNodeRelationRole());
        assertEquals(ImpactChangeType.CREATE_RELATED_NODE,
                impact.expectedChange().changeType());
        assertEquals(requiredNodeType,
                impact.expectedChange().nodeTypeToCreate());
        assertEquals(relationshipType,
                impact.expectedChange().relationTypeToCreate());
        assertEquals(targetNodeId,
                impact.expectedChange().existingNodeId());
        assertEquals(existingNodeRole,
                impact.expectedChange().existingNodeRelationRole());
        assertEquals(
                ResolutionExpectation
                        .FINDING_EXPECTED_TO_BE_RESOLVED_AFTER_VALID_COMPLETION,
                impact.expectedChange().resolutionExpectation()
        );
        assertEquals(1, impact.executionWave());
        assertTrue(impact.dependsOn().isEmpty());
    }

    private List<RemediationTask> supportedTasks() {
        return List.of(
                task(
                        "TASK-TEST",
                        RemediationTaskType.CREATE_TEST_IMPLEMENTATION,
                        FindingCode.SCENARIO_WITHOUT_TEST,
                        "SC-001",
                        "SCENARIO",
                        List.of()
                ),
                task(
                        "TASK-CHECK",
                        RemediationTaskType.CREATE_CHECK,
                        FindingCode.TEST_WITHOUT_CHECK,
                        "TC-001",
                        "TEST_IMPLEMENTATION",
                        List.of()
                ),
                task(
                        "TASK-SCENARIO",
                        RemediationTaskType.CREATE_SCENARIO,
                        FindingCode.BUSINESS_RULE_WITHOUT_SCENARIO,
                        "BR-001",
                        "BUSINESS_RULE",
                        List.of()
                )
        );
    }

    private RemediationTask task(
            String id,
            RemediationTaskType type,
            FindingCode findingCode,
            String targetNodeId,
            String targetNodeType,
            List<String> dependencies
    ) {
        return new RemediationTask(
                id,
                type,
                RemediationTaskStatus.PLANNED,
                findingCode,
                targetNodeId,
                targetNodeType,
                "Description",
                dependencies
        );
    }

    private RoadmapReport roadmap(List<RemediationTask> tasks) {
        int planned = (int) tasks.stream()
                .filter(task -> task.status() == RemediationTaskStatus.PLANNED)
                .count();
        int withDependencies = (int) tasks.stream()
                .filter(task -> !task.dependsOn().isEmpty())
                .count();
        return new RoadmapReport(
                true,
                "0.1",
                new RoadmapSummary(tasks.size(), planned, withDependencies),
                tasks,
                new FindingsSummary(tasks.size(), 0, tasks.size(), 0)
        );
    }

    private ExecutionPlan plan(
            RoadmapReport roadmap,
            List<ExecutionWave> waves
    ) {
        int totalTasks = waves.stream()
                .mapToInt(wave -> wave.taskIds().size()).sum();
        int parallelizable = waves.stream()
                .filter(wave -> wave.taskIds().size() > 1)
                .mapToInt(wave -> wave.taskIds().size()).sum();
        int sequential = (int) waves.stream()
                .filter(wave -> wave.taskIds().size() == 1).count();
        int maximumParallelism = waves.stream()
                .mapToInt(wave -> wave.taskIds().size()).max().orElse(0);
        return new ExecutionPlan(
                true,
                roadmap.schemaVersion(),
                new ExecutionPlanSummary(
                        totalTasks,
                        waves.size(),
                        parallelizable,
                        sequential,
                        maximumParallelism
                ),
                waves,
                roadmap.summary()
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
                "Message",
                "Recommendation"
        );
    }

    private void assertFailure(
            ImpactAnalysisErrorCode code,
            String messageFragment,
            Runnable action
    ) {
        ImpactAnalysisException exception = assertThrows(
                ImpactAnalysisException.class,
                action::run
        );
        assertEquals(code, exception.code());
        assertTrue(exception.getMessage().contains(messageFragment));
        assertFalse(exception.getMessage().isBlank());
    }
}
