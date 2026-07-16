package ru.kuznetsov.qaip.core.engine;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class AnalysisEngineRegistry {

    private final List<AnalysisEngine> engines;

    public AnalysisEngineRegistry(
            List<AnalysisEngine> engines
    ) {
        Objects.requireNonNull(
                engines,
                "engines must not be null"
        );

        this.engines = engines.stream()
                .sorted(Comparator
                        .comparingInt(AnalysisEngine::order)
                        .thenComparing(AnalysisEngine::id))
                .toList();
    }

    public List<AnalysisEngine> engines() {
        return engines;
    }
}
