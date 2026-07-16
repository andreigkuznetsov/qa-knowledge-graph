package ru.kuznetsov.qaip.coverage.traceability.model;

import java.util.List;

public record TraceabilityChain(
        String id,
        TraceabilityNodeRef userStory,
        TraceabilityNodeRef businessOperation,
        TraceabilityNodeRef businessRule,
        TraceabilityNodeRef scenario,
        TraceabilityNodeRef testImplementation,
        TraceabilityNodeRef check,
        TraceabilityNodeRef lastNode,
        List<String> nodeIds,
        int depth
) {
}
