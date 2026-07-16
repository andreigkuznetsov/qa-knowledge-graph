package ru.kuznetsov.qaip.analysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qaip.analysis.orchestration.AnalysisOrchestrator;
import ru.kuznetsov.qaip.analysis.orchestration.UnifiedAnalysisResult;

import java.util.Objects;

public final class UnifiedAnalysisService {

    private final AnalysisOrchestrator orchestrator;
    private final String schemaVersion;

    public UnifiedAnalysisService(
            AnalysisOrchestrator orchestrator,
            String schemaVersion
    ) {
        this.orchestrator = Objects.requireNonNull(
                orchestrator,
                "orchestrator must not be null"
        );
        this.schemaVersion = Objects.requireNonNull(
                schemaVersion,
                "schemaVersion must not be null"
        );
    }

    public UnifiedAnalysisResult analyze(
            JsonNode qaModel
    ) {
        return orchestrator.analyze(
                qaModel,
                schemaVersion
        );
    }
}
