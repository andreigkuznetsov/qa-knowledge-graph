package ru.kuznetsov.qagraph.api;

public record ExecutionPlanSummaryResponse(
        int totalTasks,
        int totalWaves,
        int parallelizableTasks,
        int sequentialTasks,
        int maximumParallelism
) {
}
