package ru.kuznetsov.qaip.core.quality;

import java.util.Map;

public record QualityAssessment(
        double score,
        QualityStatus status,
        Map<String, Object> factors
) {
    public QualityAssessment {
        if (score < 0.0 || score > 100.0) {
            throw new IllegalArgumentException(
                    "score must be between 0 and 100"
            );
        }

        factors = factors == null
                ? Map.of()
                : Map.copyOf(factors);
    }
}
