package ru.kuznetsov.qaip.analysis.config;

import ru.kuznetsov.qaip.analysis.adapter.validation.ValidationAnalysisEngine;
import ru.kuznetsov.qaip.analysis.execution.AnalysisExecutor;
import ru.kuznetsov.qaip.analysis.orchestration.AnalysisOrchestrator;
import ru.kuznetsov.qaip.analysis.service.UnifiedAnalysisService;
import ru.kuznetsov.qaip.core.engine.AnalysisEngine;
import ru.kuznetsov.qaip.core.engine.AnalysisEngineRegistry;
import ru.kuznetsov.qaip.core.metadata.AnalysisIdGenerator;
import ru.kuznetsov.qaip.core.metadata.DefaultAnalysisIdGenerator;
import ru.kuznetsov.qaip.core.metadata.MetadataFactory;

import java.util.List;
import java.util.Objects;

/**
 * Composition root для Unified Analysis.
 *
 * Здесь регистрируются движки, входящие в стандартный
 * аналитический pipeline QAIP 0.6.
 */
public final class UnifiedAnalysisFactory {

    public static final String RELEASE = "0.6";
    public static final String BUILD = "RC1";
    public static final String SCHEMA_VERSION = "0.1";

    private UnifiedAnalysisFactory() {
    }

    public static UnifiedAnalysisService createDefault() {
        return create(
                List.of(
                        new ValidationAnalysisEngine()
                ),
                new DefaultAnalysisIdGenerator()
        );
    }

    static UnifiedAnalysisService create(
            List<AnalysisEngine> engines,
            AnalysisIdGenerator idGenerator
    ) {
        Objects.requireNonNull(
                engines,
                "engines must not be null"
        );
        Objects.requireNonNull(
                idGenerator,
                "idGenerator must not be null"
        );

        AnalysisEngineRegistry registry =
                new AnalysisEngineRegistry(engines);

        AnalysisExecutor executor =
                new AnalysisExecutor(registry);

        MetadataFactory metadataFactory =
                new MetadataFactory(
                        RELEASE,
                        BUILD,
                        idGenerator
                );

        AnalysisOrchestrator orchestrator =
                new AnalysisOrchestrator(
                        executor,
                        metadataFactory
                );

        return new UnifiedAnalysisService(
                orchestrator,
                SCHEMA_VERSION
        );
    }
}
