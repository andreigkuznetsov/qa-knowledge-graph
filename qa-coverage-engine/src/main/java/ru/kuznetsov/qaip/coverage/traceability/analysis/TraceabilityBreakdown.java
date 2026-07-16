package ru.kuznetsov.qaip.coverage.traceability.analysis;

public record TraceabilityBreakdown(
        int fullyTraceable,
        int brokenAtOperation,
        int brokenAtRule,
        int brokenAtScenario,
        int brokenAtTestImplementation,
        int brokenAtCheck
) {
}
