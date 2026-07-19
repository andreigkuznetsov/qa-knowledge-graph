package ru.kuznetsov.qagraph.api;

public record AssessmentValidationSummaryResponse(
        boolean valid,
        int errors,
        int warnings
) {
}
