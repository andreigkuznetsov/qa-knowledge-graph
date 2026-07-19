package ru.kuznetsov.qagraph.api;

import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;
import ru.kuznetsov.qaip.coverage.model.CoverageProblem;

import java.util.List;

public record RegisteredModelCoverageResponse(
        String modelId,
        boolean analyzed,
        String schemaVersion,
        List<CoverageMetricResponse> metrics,
        List<CoverageProblem> problems,
        QaModelValidationResult validation
) {
    public RegisteredModelCoverageResponse {
        metrics = List.copyOf(metrics);
        problems = List.copyOf(problems);
    }
}
