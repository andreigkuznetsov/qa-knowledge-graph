package ru.kuznetsov.qaip.core.analysis;

import ru.kuznetsov.qaip.core.finding.AnalysisFinding;
import ru.kuznetsov.qaip.core.metric.AnalysisMetric;

import java.util.List;
import java.util.Map;

public record AnalysisAssessment(
        String engineId,
        String engineName,
        AssessmentStatus status,
        List<AnalysisFinding> findings,
        List<AnalysisMetric> metrics,
        Map<String, Object> data
) {
    public AnalysisAssessment {
        findings = findings == null
                ? List.of()
                : List.copyOf(findings);
        metrics = metrics == null
                ? List.of()
                : List.copyOf(metrics);
        data = data == null
                ? Map.of()
                : Map.copyOf(data);
    }

    public static AnalysisAssessment skipped(
            String engineId,
            String engineName,
            String reason
    ) {
        return new AnalysisAssessment(
                engineId,
                engineName,
                AssessmentStatus.SKIPPED,
                List.of(),
                List.of(),
                Map.of("reason", reason)
        );
    }
}
