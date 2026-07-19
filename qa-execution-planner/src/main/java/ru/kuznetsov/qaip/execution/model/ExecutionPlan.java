package ru.kuznetsov.qaip.execution.model;

import ru.kuznetsov.qaip.roadmap.model.RoadmapSummary;

import java.util.List;

public record ExecutionPlan(
        boolean planned,
        String schemaVersion,
        ExecutionPlanSummary summary,
        List<ExecutionWave> waves,
        RoadmapSummary sourceRoadmapSummary
) {
    public ExecutionPlan {
        waves = List.copyOf(waves);
    }
}
