package ru.kuznetsov.qaip.analysis.orchestration;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qaip.analysis.execution.AnalysisExecutionResult;
import ru.kuznetsov.qaip.analysis.execution.AnalysisExecutor;
import ru.kuznetsov.qaip.core.analysis.AnalysisContext;
import ru.kuznetsov.qaip.core.metadata.AnalysisMetadata;
import ru.kuznetsov.qaip.core.metadata.MetadataFactory;

import java.util.Objects;

public final class AnalysisOrchestrator {

    private final AnalysisExecutor executor;
    private final MetadataFactory metadataFactory;

    public AnalysisOrchestrator(
            AnalysisExecutor executor,
            MetadataFactory metadataFactory
    ) {
        this.executor = Objects.requireNonNull(
                executor,
                "executor must not be null"
        );
        this.metadataFactory = Objects.requireNonNull(
                metadataFactory,
                "metadataFactory must not be null"
        );
    }

    public UnifiedAnalysisResult analyze(
            JsonNode qaModel,
            String schemaVersion
    ) {
        Objects.requireNonNull(
                qaModel,
                "qaModel must not be null"
        );
        Objects.requireNonNull(
                schemaVersion,
                "schemaVersion must not be null"
        );

        AnalysisMetadata metadata =
                metadataFactory.create(schemaVersion);

        AnalysisContext context =
                new AnalysisContext(
                        metadata.analysisId(),
                        metadata.release(),
                        metadata.build(),
                        metadata.schemaVersion(),
                        metadata.generatedAt()
                );

        AnalysisExecutionResult execution =
                executor.execute(
                        qaModel,
                        context
                );

        return new UnifiedAnalysisResult(
                metadata,
                execution
        );
    }
}
