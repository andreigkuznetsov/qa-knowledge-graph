package ru.kuznetsov.qagraph.model;

import java.util.List;

public record QaModelValidationResponse(
        boolean valid,
        String schemaVersion,
        ValidationSummary summary,
        List<ValidationIssue> issues
) {
}
