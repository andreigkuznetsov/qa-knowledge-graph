package ru.kuznetsov.qaip.execution.model;

public record ExecutionPlanSummary(
        int totalTasks,
        int totalWaves,
        int parallelizableTasks,
        int sequentialTasks,
        int maximumParallelism
) {
}
