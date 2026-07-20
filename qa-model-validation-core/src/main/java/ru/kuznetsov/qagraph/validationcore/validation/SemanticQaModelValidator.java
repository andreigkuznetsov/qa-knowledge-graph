package ru.kuznetsov.qagraph.validationcore.validation;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;
import ru.kuznetsov.qagraph.validationcore.validation.semantic.SemanticValidationEngine;
import ru.kuznetsov.qagraph.validationcore.validation.semantic.SemanticValidationRules;

import java.util.List;

/**
 * Backward-compatible entry point for semantic QA-model validation.
 */
public class SemanticQaModelValidator {

    private final SemanticValidationEngine engine;

    public SemanticQaModelValidator() {
        this(new SemanticValidationEngine(SemanticValidationRules.defaults()));
    }

    public SemanticQaModelValidator(SemanticValidationEngine engine) {
        this.engine = engine;
    }

    public List<ValidationIssue> validate(JsonNode document) {
        return engine.validate(document);
    }
}
