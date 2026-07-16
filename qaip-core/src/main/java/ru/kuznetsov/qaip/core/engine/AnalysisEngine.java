package ru.kuznetsov.qaip.core.engine;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qaip.core.analysis.AnalysisAssessment;
import ru.kuznetsov.qaip.core.analysis.AnalysisContext;

/**
 * Общий контракт независимого аналитического движка QAIP.
 *
 * Движок не должен зависеть от других движков. Оркестрация
 * выполняется отдельным слоем платформы.
 */
public interface AnalysisEngine {

    String id();

    String name();

    default int order() {
        return 1000;
    }

    default boolean supports(JsonNode qaModel) {
        return qaModel != null && qaModel.isObject();
    }

    AnalysisAssessment analyze(
            JsonNode qaModel,
            AnalysisContext context
    );
}
