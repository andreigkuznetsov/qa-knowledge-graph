package ru.kuznetsov.qagraph.validationcore.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;
import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;

import java.util.Comparator;
import java.util.List;

public class JsonSchemaQaModelValidator {

    private final JsonSchema schema;

    public JsonSchemaQaModelValidator(JsonSchema schema) {
        this.schema = schema;
    }

    public List<ValidationIssue> validate(JsonNode document) {
        return schema.validate(document).stream()
                .map(this::toIssue)
                .sorted(Comparator.comparing(
                        issue -> issue.path() == null ? "" : issue.path()
                ))
                .toList();
    }

    private ValidationIssue toIssue(ValidationMessage message) {
        String code = message.getType() == null
                ? "JSON_SCHEMA_VIOLATION"
                : message.getType().toUpperCase();

        String path = message.getInstanceLocation() == null
                ? null
                : message.getInstanceLocation().toString();

        return ValidationIssue.schemaError(
                code,
                message.getMessage(),
                path
        );
    }
}
