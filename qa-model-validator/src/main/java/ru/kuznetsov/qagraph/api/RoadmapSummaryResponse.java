package ru.kuznetsov.qagraph.api;

public record RoadmapSummaryResponse(
        int totalTasks,
        int plannedTasks,
        int tasksWithDependencies
) {
}
