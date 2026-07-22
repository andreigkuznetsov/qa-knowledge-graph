package ru.kuznetsov.qagraph.validationcore;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;
import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;
import ru.kuznetsov.qagraph.validationcore.model.ValidationSeverity;
import ru.kuznetsov.qagraph.validationcore.model.ValidationSummary;
import ru.kuznetsov.qagraph.validationcore.validation.JsonSchemaQaModelValidator;
import ru.kuznetsov.qagraph.validationcore.validation.SemanticQaModelValidator;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class QaModelValidationEngine {

    private final JsonSchemaQaModelValidator schemaValidator;
    private final SemanticQaModelValidator semanticValidator;

    public QaModelValidationEngine() {
        this.schemaValidator = new JsonSchemaQaModelValidator(loadSchema());
        this.semanticValidator = new SemanticQaModelValidator();
    }

    public QaModelValidationResult validate(JsonNode document) {
        List<ValidationIssue> issues =
                new ArrayList<>(validateSchema(document));

        if (issues.isEmpty()) {
            issues.addAll(validateSemantic(document));
        }

        List<ValidationIssue> sortedIssues = issues.stream()
                .sorted(Comparator
                        .comparing(ValidationIssue::severity)
                        .thenComparing(ValidationIssue::layer)
                        .thenComparing(issue ->
                                issue.objectId() == null
                                        ? ""
                                        : issue.objectId())
                        .thenComparing(ValidationIssue::code))
                .toList();

        int errors = (int) sortedIssues.stream()
                .filter(issue ->
                        issue.severity() == ValidationSeverity.ERROR)
                .count();

        int warnings = (int) sortedIssues.stream()
                .filter(issue ->
                        issue.severity() == ValidationSeverity.WARNING)
                .count();

        return new QaModelValidationResult(
                errors == 0,
                document.path("schemaVersion").asText(null),
                new ValidationSummary(
                        errors,
                        warnings,
                        sortedIssues.size()
                ),
                sortedIssues
        );
    }

    /** Executes only the authoritative complete JSON Schema validator. */
    public List<ValidationIssue> validateSchema(JsonNode document) {
        return schemaValidator.validate(document);
    }

    /** Executes only the authoritative semantic v0.1 rule set. */
    public List<ValidationIssue> validateSemantic(JsonNode document) {
        return semanticValidator.validate(document);
    }

    private JsonSchema loadSchema() {
        try (InputStream inputStream =
                     QaModelValidationEngine.class
                             .getClassLoader()
                             .getResourceAsStream(
                                     "schema/qa-model-v0.1.schema.json"
                             )) {

            if (inputStream == null) {
                throw new IllegalStateException(
                        "qa-model-v0.1.schema.json not found"
                );
            }

            return JsonSchemaFactory
                    .getInstance(SpecVersion.VersionFlag.V202012)
                    .getSchema(inputStream);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Cannot load QA Model JSON Schema",
                    exception
            );
        }
    }
}
