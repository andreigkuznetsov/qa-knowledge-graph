package ru.kuznetsov.qaip.coverage.traceability.analysis;

import ru.kuznetsov.qaip.coverage.traceability.model.TraceabilityChain;

public record AnalyzedTraceabilityChain(
        TraceabilityChain chain,
        TraceabilityStatus status,
        String brokenAfterType
) {
}
