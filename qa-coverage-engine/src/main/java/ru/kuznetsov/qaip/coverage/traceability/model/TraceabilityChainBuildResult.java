package ru.kuznetsov.qaip.coverage.traceability.model;

import java.util.List;

public record TraceabilityChainBuildResult(
        int totalChains,
        List<TraceabilityChain> chains
) {
}
