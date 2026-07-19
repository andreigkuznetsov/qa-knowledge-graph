package ru.kuznetsov.qaip.roadmap.model;

public record RoadmapSummary(
        int totalTasks,
        int plannedTasks,
        int tasksWithDependencies
) {
}
