package ru.kuznetsov.qagraph.change.complete;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;

import java.util.List;

interface CompleteValidationBackend {
    List<ValidationIssue> validateSchema(JsonNode root);

    List<ValidationIssue> validateSemantic(JsonNode root);
}
