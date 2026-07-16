package ru.kuznetsov.qaip.core.analysis;

import java.time.Instant;

public record AnalysisContext(
        String analysisId,
        String platformRelease,
        String build,
        String schemaVersion,
        Instant startedAt
) {
}
