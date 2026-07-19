package ru.kuznetsov.qaip.impact.model;

import java.util.List;
import java.util.Objects;

public record ImpactReport(
        boolean analyzed,
        String schemaVersion,
        ImpactSummary summary,
        List<TaskImpact> taskImpacts
) {
    public ImpactReport {
        Objects.requireNonNull(schemaVersion,
                "schemaVersion must not be null");
        Objects.requireNonNull(summary, "summary must not be null");
        Objects.requireNonNull(taskImpacts,
                "taskImpacts must not be null");
        taskImpacts = List.copyOf(taskImpacts);
    }
}
