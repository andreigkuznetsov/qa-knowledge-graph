package ru.kuznetsov.qaip.analysis.execution;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qaip.core.analysis.AnalysisAssessment;
import ru.kuznetsov.qaip.core.analysis.AnalysisContext;
import ru.kuznetsov.qaip.core.engine.AnalysisEngine;
import ru.kuznetsov.qaip.core.engine.AnalysisEngineRegistry;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class AnalysisExecutor {

    private final AnalysisEngineRegistry registry;

    public AnalysisExecutor(AnalysisEngineRegistry registry) {
        this.registry = Objects.requireNonNull(
                registry,
                "registry must not be null"
        );
    }

    public AnalysisExecutionResult execute(
            JsonNode qaModel,
            AnalysisContext context
    ) {
        Objects.requireNonNull(
                qaModel,
                "qaModel must not be null"
        );
        Objects.requireNonNull(
                context,
                "context must not be null"
        );

        Instant executionStartedAt = Instant.now();
        List<EngineExecutionResult> results =
                new ArrayList<>();

        for (AnalysisEngine engine : registry.engines()) {
            if (!engine.supports(qaModel)) {
                AnalysisAssessment skipped =
                        AnalysisAssessment.skipped(
                                engine.id(),
                                engine.name(),
                                "Модель не поддерживается движком"
                        );

                results.add(new EngineExecutionResult(
                        engine.id(),
                        skipped,
                        Duration.ZERO
                ));
                continue;
            }

            Instant engineStartedAt = Instant.now();

            AnalysisAssessment assessment =
                    engine.analyze(
                            qaModel,
                            context
                    );

            results.add(new EngineExecutionResult(
                    engine.id(),
                    assessment,
                    Duration.between(
                            engineStartedAt,
                            Instant.now()
                    )
            ));
        }

        return new AnalysisExecutionResult(
                results,
                Duration.between(
                        executionStartedAt,
                        Instant.now()
                )
        );
    }
}
