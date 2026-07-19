package ru.kuznetsov.qaip.coverage.model;

public record CoverageMetric(
        CoverageMetricCode code,
        String name,
        int total,
        int covered,
        int uncovered,
        double percentage
) {
    public double safePercentage() {
        return total == 0 ? 0.0 : percentage;
    }
}
