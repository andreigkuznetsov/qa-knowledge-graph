package ru.kuznetsov.qaip.coverage.traceability.model;

import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;

import java.time.Instant;
import java.util.List;

public record TraceabilityChainReport(
        boolean built,
        String release,
        String schemaVersion,
        Instant generatedAt,
        int totalChains,
        List<TraceabilityChain> chains,
        QaModelValidationResult validation
) {
}
