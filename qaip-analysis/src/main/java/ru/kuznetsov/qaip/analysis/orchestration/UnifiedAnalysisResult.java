package ru.kuznetsov.qaip.analysis.orchestration;

import ru.kuznetsov.qaip.analysis.execution.AnalysisExecutionResult;
import ru.kuznetsov.qaip.core.metadata.AnalysisMetadata;

public record UnifiedAnalysisResult(
        AnalysisMetadata metadata,
        AnalysisExecutionResult execution
) {
}
