package ru.kuznetsov.qaip.analysis.adapter.validation;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qagraph.validationcore.QaModelValidationEngine;
import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;
import ru.kuznetsov.qaip.core.analysis.AnalysisAssessment;
import ru.kuznetsov.qaip.core.analysis.AnalysisContext;
import ru.kuznetsov.qaip.core.engine.AnalysisEngine;

import java.util.Objects;

public final class ValidationAnalysisEngine implements AnalysisEngine {

    private static final int ORDER = 100;

    private final QaModelValidationEngine validationEngine;
    private final ValidationAssessmentMapper mapper;

    public ValidationAnalysisEngine() {
        this(new QaModelValidationEngine(), new ValidationAssessmentMapper());
    }

    ValidationAnalysisEngine(
            QaModelValidationEngine validationEngine,
            ValidationAssessmentMapper mapper
    ) {
        this.validationEngine = Objects.requireNonNull(
                validationEngine,
                "validationEngine must not be null"
        );
        this.mapper = Objects.requireNonNull(
                mapper,
                "mapper must not be null"
        );
    }

    @Override
    public String id() {
        return ValidationAssessmentMapper.ENGINE_ID;
    }

    @Override
    public String name() {
        return ValidationAssessmentMapper.ENGINE_NAME;
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public AnalysisAssessment analyze(
            JsonNode qaModel,
            AnalysisContext context
    ) {
        Objects.requireNonNull(qaModel, "qaModel must not be null");
        Objects.requireNonNull(context, "context must not be null");

        QaModelValidationResult result = validationEngine.validate(qaModel);
        return mapper.map(result);
    }
}
