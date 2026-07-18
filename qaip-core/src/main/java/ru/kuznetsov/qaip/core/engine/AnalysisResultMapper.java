package ru.kuznetsov.qaip.core.engine;

import ru.kuznetsov.qaip.core.analysis.AnalysisAssessment;

/**
 * Преобразует специфичный результат аналитического движка
 * в единый контракт {@link AnalysisAssessment}.
 *
 * @param <T> тип результата конкретного движка
 */
@FunctionalInterface
public interface AnalysisResultMapper<T> {

    AnalysisAssessment map(T result);
}
