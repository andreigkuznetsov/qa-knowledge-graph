package ru.kuznetsov.qagraph.api;

import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;

import java.util.List;

public record RegisteredModelExecutionPlanResponse(
        String modelId,
        boolean planned,
        String schemaVersion,
        ExecutionPlanSummaryResponse summary,
        List<ExecutionWaveResponse> waves,
        RoadmapSummaryResponse sourceRoadmapSummary,
        QaModelValidationResult validation
) {
    public RegisteredModelExecutionPlanResponse {
        waves = List.copyOf(waves);
    }
}
