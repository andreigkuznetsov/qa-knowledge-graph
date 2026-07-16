package ru.kuznetsov.qagraph.extractor.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;
import ru.kuznetsov.qagraph.extractor.model.ExtractionIssue;

import java.util.Comparator;
import java.util.List;

public class JsonDocumentSchemaValidator {

    private final JsonSchema schema;
    private final boolean inputSchema;

    public JsonDocumentSchemaValidator(JsonSchema schema, boolean inputSchema) {
        this.schema = schema;
        this.inputSchema = inputSchema;
    }

    public List<ExtractionIssue> validate(JsonNode document) {
        return schema.validate(document).stream()
                .map(this::toIssue)
                .sorted(Comparator.comparing(issue ->
                        issue.path() == null ? "" : issue.path()))
                .toList();
    }

    private ExtractionIssue toIssue(ValidationMessage message) {
        String path = message.getInstanceLocation() == null
                ? null
                : message.getInstanceLocation().toString();

        String code = message.getType() == null
                ? "JSON_SCHEMA_VALIDATION_ERROR"
                : message.getType().toUpperCase();

        return inputSchema
                ? ExtractionIssue.schemaError(code, message.getMessage(), path)
                : ExtractionIssue.outputError(code, message.getMessage(), path);
    }
}
