package ru.kuznetsov.qaip.coverage.traceability.analysis;

import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;
import java.time.Instant;
import java.util.List;

public record TraceabilityCoverageReport(
        boolean analyzed,
        String release,
        String schemaVersion,
        Instant generatedAt,
        TraceabilityCoverageSummary summary,
        TraceabilityBreakdown breakdown,
        List<AnalyzedTraceabilityChain> chains,
        List<TraceabilityProblem> problems,
        QaModelValidationResult validation
) {
}
