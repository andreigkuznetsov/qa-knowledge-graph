package ru.kuznetsov.qagraph.extractor.model;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;

import java.util.List;

public record ExtractAndValidateResponse(
        boolean success,
        ExtractionResult extraction,
        ValidationResult validation,
        JsonNode qaModel
) {
    public record ExtractionResult(
            boolean extracted,
            int errors,
            int warnings
    ) {
    }

    public record ValidationResult(
            boolean valid,
            String schemaVersion,
            int errors,
            int warnings,
            List<ValidationIssue> issues
    ) {
    }
}
