package ru.kuznetsov.qaip.core.finding;

import java.util.Map;

public record AnalysisFinding(
        String engineId,
        FindingSeverity severity,
        FindingCategory category,
        String code,
        String message,
        String objectId,
        String path,
        String recommendation,
        Map<String, Object> details
) {
    public AnalysisFinding {
        details = details == null
                ? Map.of()
                : Map.copyOf(details);
    }
}
