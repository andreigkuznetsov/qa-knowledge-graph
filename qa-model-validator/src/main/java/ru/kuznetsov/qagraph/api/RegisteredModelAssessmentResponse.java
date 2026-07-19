package ru.kuznetsov.qagraph.api;

import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;

public record RegisteredModelAssessmentResponse(
        String modelId,
        boolean analyzed,
        String schemaVersion,
        AssessmentHealth health,
        AssessmentSummaryResponse summary,
        RegisteredModelCoverageResponse coverage,
        RegisteredModelFindingsResponse findings,
        QaModelValidationResult validation
) {
}
