package ru.kuznetsov.qaip.analysis.execution;

import ru.kuznetsov.qaip.core.analysis.AnalysisAssessment;
import java.time.Duration;

public record EngineExecutionResult(
        String engineId,
        AnalysisAssessment assessment,
        Duration duration
) {
}
