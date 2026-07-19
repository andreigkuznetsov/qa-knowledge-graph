package ru.kuznetsov.qagraph.api;

import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;

import java.util.List;

public record RegisteredModelFindingsResponse(
        String modelId,
        boolean analyzed,
        String schemaVersion,
        FindingsSummaryResponse summary,
        List<FindingResponse> findings,
        QaModelValidationResult validation
) {
    public RegisteredModelFindingsResponse {
        findings = List.copyOf(findings);
    }
}
