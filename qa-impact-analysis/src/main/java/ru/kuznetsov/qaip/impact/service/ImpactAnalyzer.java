package ru.kuznetsov.qaip.impact.service;

import ru.kuznetsov.qagraph.model.NodeType;
import ru.kuznetsov.qaip.execution.model.ExecutionPlan;
import ru.kuznetsov.qaip.execution.model.ExecutionWave;
import ru.kuznetsov.qaip.impact.mapping.RemediationImpactCatalog;
import ru.kuznetsov.qaip.impact.mapping.RemediationImpactDefinition;
import ru.kuznetsov.qaip.impact.model.ExpectedStructuralChange;
import ru.kuznetsov.qaip.impact.model.ImpactReport;
import ru.kuznetsov.qaip.impact.model.ImpactSummary;
import ru.kuznetsov.qaip.impact.model.StructuralGap;
import ru.kuznetsov.qaip.impact.model.TaskImpact;
import ru.kuznetsov.qaip.roadmap.model.RemediationTask;
import ru.kuznetsov.qaip.roadmap.model.RemediationTaskStatus;
import ru.kuznetsov.qaip.roadmap.model.RoadmapReport;
import ru.kuznetsov.qaip.roadmap.model.RoadmapSummary;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ImpactAnalyzer {

    public ImpactReport analyze(
            RoadmapReport roadmapReport,
            ExecutionPlan executionPlan
    ) {
        Objects.requireNonNull(roadmapReport,
                "roadmapReport must not be null");
        Objects.requireNonNull(executionPlan,
                "executionPlan must not be null");

        validateReportHeaders(roadmapReport, executionPlan);
        Map<String, RemediationTask> tasks = indexTasks(roadmapReport);
        validateRoadmapSummary(roadmapReport, tasks.values());

        List<TaskImpact> impacts = new ArrayList<>();
        Set<String> assignedTaskIds = new HashSet<>();
        Set<Integer> affectedWaves = new HashSet<>();

        for (int index = 0; index < executionPlan.waves().size(); index++) {
            ExecutionWave wave = Objects.requireNonNull(
                    executionPlan.waves().get(index),
                    "execution wave must not be null"
            );
            int expectedNumber = index + 1;
            if (wave.number() != expectedNumber) {
                throw failure(
                        ImpactAnalysisErrorCode.INVALID_EXECUTION_WAVE,
                        "Execution wave number must be " + expectedNumber
                                + " but was " + wave.number()
                );
            }
            if (wave.taskIds().isEmpty()) {
                throw failure(
                        ImpactAnalysisErrorCode.INVALID_EXECUTION_WAVE,
                        "Execution wave must contain at least one task: "
                                + wave.number()
                );
            }
            affectedWaves.add(wave.number());

            for (String taskId : wave.taskIds()) {
                Objects.requireNonNull(taskId, "task ID must not be null");
                RemediationTask task = tasks.get(taskId);
                if (task == null) {
                    throw failure(
                            ImpactAnalysisErrorCode.UNKNOWN_EXECUTION_TASK,
                            "Execution plan contains unknown task ID: "
                                    + taskId
                    );
                }
                if (!assignedTaskIds.add(taskId)) {
                    throw failure(
                            ImpactAnalysisErrorCode.DUPLICATE_EXECUTION_TASK,
                            "Task appears more than once in execution plan: "
                                    + taskId
                    );
                }
                impacts.add(toImpact(task, wave.number()));
            }
        }

        for (String taskId : tasks.keySet()) {
            if (!assignedTaskIds.contains(taskId)) {
                throw failure(
                        ImpactAnalysisErrorCode.ROADMAP_TASK_NOT_SCHEDULED,
                        "Roadmap task is not present in execution plan: "
                                + taskId
                );
            }
        }

        return new ImpactReport(
                true,
                roadmapReport.schemaVersion(),
                summarize(
                        impacts,
                        tasks.values(),
                        affectedWaves.size(),
                        executionPlan.summary().parallelizableTasks()
                ),
                impacts
        );
    }

    private void validateReportHeaders(
            RoadmapReport roadmap,
            ExecutionPlan plan
    ) {
        Objects.requireNonNull(roadmap.schemaVersion(),
                "roadmap schemaVersion must not be null");
        Objects.requireNonNull(plan.schemaVersion(),
                "execution plan schemaVersion must not be null");
        Objects.requireNonNull(roadmap.summary(),
                "roadmap summary must not be null");
        Objects.requireNonNull(plan.summary(),
                "execution plan summary must not be null");
        Objects.requireNonNull(plan.sourceRoadmapSummary(),
                "sourceRoadmapSummary must not be null");

        if (!roadmap.planned() || !plan.planned()) {
            throw failure(
                    ImpactAnalysisErrorCode.INCONSISTENT_INPUT,
                    "Roadmap and execution plan must both be planned"
            );
        }
        if (!roadmap.summary().equals(plan.sourceRoadmapSummary())) {
            throw failure(
                    ImpactAnalysisErrorCode.INCONSISTENT_INPUT,
                    "Execution plan source roadmap summary does not match roadmap"
            );
        }
    }

    private Map<String, RemediationTask> indexTasks(RoadmapReport roadmap) {
        Map<String, RemediationTask> tasks = new LinkedHashMap<>();
        for (RemediationTask task : roadmap.tasks()) {
            Objects.requireNonNull(task, "roadmap task must not be null");
            String taskId = Objects.requireNonNull(
                    task.id(),
                    "roadmap task ID must not be null"
            );
            if (tasks.putIfAbsent(taskId, task) != null) {
                throw failure(
                        ImpactAnalysisErrorCode.DUPLICATE_ROADMAP_TASK_ID,
                        "Roadmap contains duplicate task ID: " + taskId
                );
            }
        }
        return tasks;
    }

    private void validateRoadmapSummary(
            RoadmapReport roadmap,
            Iterable<RemediationTask> tasks
    ) {
        int total = 0;
        int planned = 0;
        int withDependencies = 0;
        for (RemediationTask task : tasks) {
            total++;
            if (task.status() == RemediationTaskStatus.PLANNED) {
                planned++;
            }
            if (!task.dependsOn().isEmpty()) {
                withDependencies++;
            }
        }
        RoadmapSummary actual = new RoadmapSummary(
                total,
                planned,
                withDependencies
        );
        if (!actual.equals(roadmap.summary())) {
            throw failure(
                    ImpactAnalysisErrorCode.INCONSISTENT_INPUT,
                    "Roadmap summary does not match roadmap tasks"
            );
        }
    }

    private TaskImpact toImpact(RemediationTask task, int waveNumber) {
        Objects.requireNonNull(task.type(), "task type must not be null");
        Objects.requireNonNull(task.targetNodeId(),
                "targetNodeId must not be null");
        Objects.requireNonNull(task.sourceFindingCode(),
                "sourceFindingCode must not be null");
        Objects.requireNonNull(task.targetNodeType(),
                "targetNodeType must not be null");

        RemediationImpactDefinition definition =
                RemediationImpactCatalog.definitionFor(task.type());
        if (definition == null) {
            throw failure(
                    ImpactAnalysisErrorCode.UNSUPPORTED_REMEDIATION_TASK_TYPE,
                    "Unsupported remediation task type: " + task.type()
            );
        }
        if (task.sourceFindingCode() != definition.sourceFindingCode()
                || !definition.affectedNodeType().name()
                        .equals(task.targetNodeType())) {
            throw failure(
                    ImpactAnalysisErrorCode.INCONSISTENT_INPUT,
                    "Roadmap task does not match impact definition: "
                            + task.id()
            );
        }

        StructuralGap gap = new StructuralGap(
                definition.affectedNodeType(),
                definition.requiredNodeType(),
                definition.requiredRelationshipType(),
                definition.affectedNodeRelationRole()
        );
        ExpectedStructuralChange change = new ExpectedStructuralChange(
                definition.changeType(),
                definition.requiredNodeType(),
                definition.requiredRelationshipType(),
                task.targetNodeId(),
                definition.affectedNodeRelationRole(),
                definition.resolutionExpectation()
        );
        return new TaskImpact(
                task.id(),
                task.type(),
                task.targetNodeId(),
                task.sourceFindingCode(),
                gap,
                change,
                waveNumber,
                task.dependsOn()
        );
    }

    private ImpactSummary summarize(
            List<TaskImpact> impacts,
            Iterable<RemediationTask> tasks,
            int wavesAffected,
            int parallelizableTasks
    ) {
        Set<String> businessRules = new HashSet<>();
        Set<String> scenarios = new HashSet<>();
        Set<String> testImplementations = new HashSet<>();
        int tasksWithDependencies = 0;

        for (TaskImpact impact : impacts) {
            NodeType type = impact.structuralGap().affectedNodeType();
            switch (type) {
                case BUSINESS_RULE -> businessRules.add(impact.targetNodeId());
                case SCENARIO -> scenarios.add(impact.targetNodeId());
                case TEST_IMPLEMENTATION ->
                        testImplementations.add(impact.targetNodeId());
                default -> throw failure(
                        ImpactAnalysisErrorCode.INCONSISTENT_INPUT,
                        "Unsupported affected node type: " + type
                );
            }
        }
        for (RemediationTask task : tasks) {
            for (String dependency : task.dependsOn()) {
                Objects.requireNonNull(dependency,
                        "task dependency ID must not be null");
            }
            if (!task.dependsOn().isEmpty()) {
                tasksWithDependencies++;
            }
        }

        return new ImpactSummary(
                impacts.size(),
                impacts.size(),
                businessRules.size(),
                scenarios.size(),
                testImplementations.size(),
                wavesAffected,
                tasksWithDependencies,
                parallelizableTasks
        );
    }

    private ImpactAnalysisException failure(
            ImpactAnalysisErrorCode code,
            String message
    ) {
        return new ImpactAnalysisException(code, message);
    }
}
