package ru.kuznetsov.qaip.coverage.model;

public record CoverageSummary(
        int totalRules,
        int coveredRules,
        int uncoveredRules,
        double ruleCoveragePercentage,
        int totalScenarios,
        int coveredScenarios,
        int uncoveredScenarios,
        double scenarioCoveragePercentage,
        int totalTests,
        int coveredTests,
        int uncoveredTests,
        double checkCoveragePercentage,
        int problems
) {
}
