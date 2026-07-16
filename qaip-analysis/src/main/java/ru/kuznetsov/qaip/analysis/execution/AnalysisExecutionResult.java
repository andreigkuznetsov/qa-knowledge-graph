package ru.kuznetsov.qaip.analysis.execution;

import java.time.Duration;
import java.util.List;

public record AnalysisExecutionResult(
        List<EngineExecutionResult> engineResults,
        Duration totalDuration
) {
    public AnalysisExecutionResult {
        engineResults = engineResults == null
                ? List.of()
                : List.copyOf(engineResults);
    }
}
