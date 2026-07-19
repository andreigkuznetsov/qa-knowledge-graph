package ru.kuznetsov.qagraph.api;

public record CoverageMetricResponse(
        String metric,
        int total,
        int covered,
        int uncovered,
        double coveragePercent
) {
}
