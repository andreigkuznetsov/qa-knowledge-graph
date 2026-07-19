package ru.kuznetsov.qaip.impact.model;

public record ImpactSummary(
        int totalTasks,
        int addressedFindingInstances,
        int businessRulesAffected,
        int scenariosAffected,
        int testImplementationsAffected,
        int wavesAffected,
        int tasksWithDependencies,
        int parallelizableTasks
) {
}
