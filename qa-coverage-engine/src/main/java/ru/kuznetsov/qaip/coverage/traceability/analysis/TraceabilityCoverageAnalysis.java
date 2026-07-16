package ru.kuznetsov.qaip.coverage.traceability.analysis;

import java.util.List;

public record TraceabilityCoverageAnalysis(
        TraceabilityCoverageSummary summary,
        TraceabilityBreakdown breakdown,
        List<AnalyzedTraceabilityChain> chains,
        List<TraceabilityProblem> problems
) {
}
