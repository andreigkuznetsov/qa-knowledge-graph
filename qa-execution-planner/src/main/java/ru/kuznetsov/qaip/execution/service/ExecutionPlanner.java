package ru.kuznetsov.qaip.execution.service;

import ru.kuznetsov.qaip.execution.model.ExecutionPlan;
import ru.kuznetsov.qaip.execution.model.ExecutionPlanSummary;
import ru.kuznetsov.qaip.execution.model.ExecutionWave;
import ru.kuznetsov.qaip.roadmap.model.RemediationTask;
import ru.kuznetsov.qaip.roadmap.model.RoadmapReport;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class ExecutionPlanner {

    private static final Comparator<RemediationTask> TASK_ORDER =
            Comparator.comparing(RemediationTask::type)
                    .thenComparing(RemediationTask::targetNodeId)
                    .thenComparing(RemediationTask::id);

    public ExecutionPlan plan(RoadmapReport roadmapReport) {
        Objects.requireNonNull(roadmapReport, "roadmapReport must not be null");

        Map<String, RemediationTask> tasks = indexTasks(roadmapReport.tasks());
        Map<String, Set<String>> dependencies =
                normalizeAndValidateDependencies(tasks);
        List<ExecutionWave> waves = buildWaves(tasks, dependencies);

        return new ExecutionPlan(
                true,
                roadmapReport.schemaVersion(),
                summarize(waves),
                waves,
                roadmapReport.summary()
        );
    }

    private Map<String, RemediationTask> indexTasks(
            List<RemediationTask> sourceTasks
    ) {
        List<RemediationTask> ordered = sourceTasks.stream()
                .peek(task -> Objects.requireNonNull(task, "task must not be null"))
                .sorted(Comparator.comparing(task ->
                        Objects.requireNonNull(task.id(), "task ID must not be null")))
                .toList();
        Map<String, RemediationTask> tasks = new LinkedHashMap<>();

        for (RemediationTask task : ordered) {
            if (tasks.putIfAbsent(task.id(), task) != null) {
                throw new DuplicateRoadmapTaskIdException(task.id());
            }
        }
        return tasks;
    }

    private Map<String, Set<String>> normalizeAndValidateDependencies(
            Map<String, RemediationTask> tasks
    ) {
        Map<String, Set<String>> dependencies = new LinkedHashMap<>();

        for (RemediationTask task : tasks.values()) {
            Set<String> uniqueDependencies =
                    new TreeSet<>(task.dependsOn());
            if (uniqueDependencies.contains(task.id())) {
                throw new SelfDependentTaskException(task.id());
            }
            dependencies.put(task.id(), uniqueDependencies);
        }

        for (RemediationTask task : tasks.values()) {
            for (String dependencyId : dependencies.get(task.id())) {
                if (!tasks.containsKey(dependencyId)) {
                    throw new UnknownTaskDependencyException(
                            task.id(),
                            dependencyId
                    );
                }
            }
        }
        return dependencies;
    }

    private List<ExecutionWave> buildWaves(
            Map<String, RemediationTask> tasks,
            Map<String, Set<String>> dependencies
    ) {
        Map<String, Integer> unresolvedCounts = new HashMap<>();
        Map<String, Set<String>> dependents = new HashMap<>();

        for (RemediationTask task : tasks.values()) {
            Set<String> taskDependencies = dependencies.get(task.id());
            unresolvedCounts.put(task.id(), taskDependencies.size());
            for (String dependencyId : taskDependencies) {
                dependents.computeIfAbsent(
                                dependencyId,
                                ignored -> new TreeSet<>()
                        )
                        .add(task.id());
            }
        }

        List<RemediationTask> ready = tasks.values().stream()
                .filter(task -> unresolvedCounts.get(task.id()) == 0)
                .sorted(TASK_ORDER)
                .toList();
        List<ExecutionWave> waves = new ArrayList<>();
        Set<String> plannedTaskIds = new HashSet<>();

        while (!ready.isEmpty()) {
            List<String> waveTaskIds = ready.stream()
                    .map(RemediationTask::id)
                    .toList();
            waves.add(new ExecutionWave(waves.size() + 1, waveTaskIds));
            plannedTaskIds.addAll(waveTaskIds);

            Set<String> nextTaskIds = new LinkedHashSet<>();
            for (String completedTaskId : waveTaskIds) {
                for (String dependentId : dependents.getOrDefault(
                        completedTaskId,
                        Set.of()
                )) {
                    int remaining = unresolvedCounts.compute(
                            dependentId,
                            (ignored, count) -> count - 1
                    );
                    if (remaining == 0) {
                        nextTaskIds.add(dependentId);
                    }
                }
            }
            ready = nextTaskIds.stream()
                    .map(tasks::get)
                    .sorted(TASK_ORDER)
                    .toList();
        }

        if (plannedTaskIds.size() != tasks.size()) {
            List<String> unresolved = tasks.keySet().stream()
                    .filter(taskId -> !plannedTaskIds.contains(taskId))
                    .sorted()
                    .toList();
            throw new CyclicTaskDependencyException(unresolved);
        }
        return List.copyOf(waves);
    }

    private ExecutionPlanSummary summarize(List<ExecutionWave> waves) {
        int totalTasks = waves.stream()
                .mapToInt(wave -> wave.taskIds().size())
                .sum();
        int parallelizable = waves.stream()
                .filter(wave -> wave.taskIds().size() > 1)
                .mapToInt(wave -> wave.taskIds().size())
                .sum();
        int sequential = waves.stream()
                .filter(wave -> wave.taskIds().size() == 1)
                .mapToInt(wave -> wave.taskIds().size())
                .sum();
        int maximumParallelism = waves.stream()
                .mapToInt(wave -> wave.taskIds().size())
                .max()
                .orElse(0);
        return new ExecutionPlanSummary(
                totalTasks,
                waves.size(),
                parallelizable,
                sequential,
                maximumParallelism
        );
    }
}
