package ru.kuznetsov.qaip.core.metric;

import java.util.Map;

public record AnalysisMetric(
        String engineId,
        String code,
        String name,
        double value,
        String unit,
        Map<String, Object> details
) {
    public AnalysisMetric {
        details = details == null
                ? Map.of()
                : Map.copyOf(details);
    }
}
