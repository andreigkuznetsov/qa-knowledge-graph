package ru.kuznetsov.qagraph.change.complete;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qagraph.validationcore.QaModelValidationEngine;
import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;

import java.util.List;
import java.util.Objects;

final class ValidationCoreBackend implements CompleteValidationBackend {
    private final QaModelValidationEngine engine;

    ValidationCoreBackend(QaModelValidationEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine must not be null");
    }

    @Override
    public List<ValidationIssue> validateSchema(JsonNode root) {
        return engine.validateSchema(root);
    }

    @Override
    public List<ValidationIssue> validateSemantic(JsonNode root) {
        return engine.validateSemantic(root);
    }
}
