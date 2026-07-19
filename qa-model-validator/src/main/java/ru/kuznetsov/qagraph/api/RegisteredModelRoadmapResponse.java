package ru.kuznetsov.qagraph.api;

import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;

import java.util.List;

public record RegisteredModelRoadmapResponse(
        String modelId,
        boolean planned,
        String schemaVersion,
        RoadmapSummaryResponse summary,
        List<RemediationTaskResponse> tasks,
        FindingsSummaryResponse sourceFindingsSummary,
        QaModelValidationResult validation
) {
    public RegisteredModelRoadmapResponse {
        tasks = List.copyOf(tasks);
    }
}
