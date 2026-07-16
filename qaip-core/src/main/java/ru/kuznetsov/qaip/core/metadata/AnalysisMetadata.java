package ru.kuznetsov.qaip.core.metadata;

import java.time.Instant;

public record AnalysisMetadata(
        String platform,
        String release,
        String build,
        String analysisId,
        String schemaVersion,
        Instant generatedAt
) {
}
