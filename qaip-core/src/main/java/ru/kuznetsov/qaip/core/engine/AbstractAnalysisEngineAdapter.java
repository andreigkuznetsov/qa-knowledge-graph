package ru.kuznetsov.qaip.core.engine;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qaip.core.analysis.AnalysisAssessment;
import ru.kuznetsov.qaip.core.analysis.AnalysisContext;

import java.util.Objects;

/**
 * Базовый адаптер для подключения неоднородных аналитических
 * движков к единому контракту QAIP.
 *
 * <p>Шаблон выполнения неизменяем:</p>
 *
 * <ol>
 *     <li>проверить входную модель и контекст;</li>
 *     <li>выполнить специфичную логику движка;</li>
 *     <li>преобразовать специфичный результат в
 *     {@link AnalysisAssessment}.</li>
 * </ol>
 *
 * @param <T> тип результата конкретного аналитического движка
 */
public abstract class AbstractAnalysisEngineAdapter<T>
        implements AnalysisEngine {

    private final AnalysisResultMapper<T> mapper;

    protected AbstractAnalysisEngineAdapter(
            AnalysisResultMapper<T> mapper
    ) {
        this.mapper = Objects.requireNonNull(
                mapper,
                "mapper must not be null"
        );
    }

    @Override
    public final AnalysisAssessment analyze(
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

        T result = Objects.requireNonNull(
                execute(qaModel, context),
                "engine result must not be null"
        );

        return Objects.requireNonNull(
                mapper.map(result),
                "analysis assessment must not be null"
        );
    }

    /**
     * Выполняет специфичную логику конкретного движка.
     */
    protected abstract T execute(
            JsonNode qaModel,
            AnalysisContext context
    );
}
