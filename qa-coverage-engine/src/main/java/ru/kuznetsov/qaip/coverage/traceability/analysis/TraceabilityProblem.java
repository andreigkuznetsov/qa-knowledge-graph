package ru.kuznetsov.qaip.coverage.traceability.analysis;

import java.util.List;

public record TraceabilityProblem(
        String chainId,
        String storyId,
        TraceabilityStatus status,
        String brokenAfterNodeId,
        String brokenAfterNodeType,
        String message,
        String recommendation,
        List<String> nodeIds
) {
}
