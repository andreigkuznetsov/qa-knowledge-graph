package ru.kuznetsov.qaip.analysis.adapter.validation;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qagraph.validationcore.QaModelValidationEngine;
import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;
import ru.kuznetsov.qaip.core.analysis.AnalysisContext;
import ru.kuznetsov.qaip.core.engine.AbstractAnalysisEngineAdapter;

import java.util.Objects;

public final class ValidationAnalysisEngine
        extends AbstractAnalysisEngineAdapter<QaModelValidationResult> {

    private static final int ORDER = 100;

    private final QaModelValidationEngine validationEngine;

    public ValidationAnalysisEngine() {
        this(new QaModelValidationEngine(), new ValidationAssessmentMapper());
    }

    ValidationAnalysisEngine(
            QaModelValidationEngine validationEngine,
            ValidationAssessmentMapper mapper
    ) {
        super(mapper);
        this.validationEngine = Objects.requireNonNull(
                validationEngine,
                "validationEngine must not be null"
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
    protected QaModelValidationResult execute(
            JsonNode qaModel,
            AnalysisContext context
    ) {
        return validationEngine.validate(qaModel);
    }
}
