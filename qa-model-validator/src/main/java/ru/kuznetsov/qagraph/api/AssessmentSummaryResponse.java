package ru.kuznetsov.qagraph.api;

public record AssessmentSummaryResponse(
        AssessmentValidationSummaryResponse validation,
        AssessmentCoverageSummaryResponse coverage,
        FindingsSummaryResponse findings
) {
}
