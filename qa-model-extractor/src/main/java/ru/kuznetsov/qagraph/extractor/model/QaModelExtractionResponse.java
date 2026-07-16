package ru.kuznetsov.qagraph.extractor.model;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record QaModelExtractionResponse(
        boolean extracted,
        String schemaVersion,
        JsonNode qaModel,
        ExtractionSummary summary,
        List<ExtractionIssue> issues
) {
}
