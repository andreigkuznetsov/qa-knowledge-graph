package ru.kuznetsov.qaip.coverage.traceability.analysis;

public record TraceabilityCoverageSummary(
        int totalChains,
        int fullyTraceableChains,
        int brokenChains,
        double coveragePercentage,
        double averageDepth,
        int maximumDepth
) {
}
