package ru.kuznetsov.qagraph.api;

public record AssessmentCoverageSummaryResponse(
        double ruleScenarioCoverage,
        double scenarioTestCoverage,
        double testCheckCoverage
) {
}
