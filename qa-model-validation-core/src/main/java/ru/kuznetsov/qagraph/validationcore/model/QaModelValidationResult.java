package ru.kuznetsov.qagraph.validationcore.model;

import java.util.List;

public record QaModelValidationResult(
        boolean valid,
        String schemaVersion,
        ValidationSummary summary,
        List<ValidationIssue> issues
) {
}
