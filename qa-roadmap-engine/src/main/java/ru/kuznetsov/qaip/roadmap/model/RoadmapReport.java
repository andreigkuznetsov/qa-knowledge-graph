package ru.kuznetsov.qaip.roadmap.model;

import ru.kuznetsov.qaip.findings.model.FindingsSummary;

import java.util.List;

public record RoadmapReport(
        boolean planned,
        String schemaVersion,
        RoadmapSummary summary,
        List<RemediationTask> tasks,
        FindingsSummary sourceFindingsSummary
) {
    public RoadmapReport {
        tasks = List.copyOf(tasks);
    }
}
