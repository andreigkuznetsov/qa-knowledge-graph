package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class ScenarioManifestSchemaValidator {
    public static final String SCHEMA_CONTRACT_IDENTIFIER =
            "qaip-scenario-authority-manifest-schema-v1";
    public static final String SCHEMA_RESOURCE =
            "/schemas/qaip-scenario-authority-manifest-v1.schema.json";

    private static final Comparator<ScenarioManifestSchemaValidationResult.Diagnostic> DIAGNOSTIC_ORDER =
            Comparator.comparing(ScenarioManifestSchemaValidationResult.Diagnostic::instanceLocation)
                    .thenComparing(ScenarioManifestSchemaValidationResult.Diagnostic::keyword)
                    .thenComparing(ScenarioManifestSchemaValidationResult.Diagnostic::message);

    private final JsonSchema schema;

    public ScenarioManifestSchemaValidator() {
        this.schema = loadSchema();
    }

    public ScenarioManifestSchemaValidationResult validate(
            ScenarioManifestJsonParseResult.Completed parsed) {
        Objects.requireNonNull(parsed, "parsed");
        List<ScenarioManifestSchemaValidationResult.ValidatedMember> members = new ArrayList<>();

        for (ScenarioManifestJsonParseResult.ParsedMember member : parsed.members()) {
            String path = member.source().repositoryRelativePath();
            List<ScenarioManifestSchemaValidationResult.Diagnostic> diagnostics = validateMessages(member.document())
                    .stream()
                    .map(message -> legacyDiagnostic(path, message))
                    .sorted(DIAGNOSTIC_ORDER)
                    .toList();
            members.add(new ScenarioManifestSchemaValidationResult.ValidatedMember(member, diagnostics));
        }
        return new ScenarioManifestSchemaValidationResult(members);
    }

    List<SchemaDiagnosticData> validateDocument(JsonNode document) {
        Objects.requireNonNull(document, "document");
        return validateMessages(document).stream()
                .map(ScenarioManifestSchemaValidator::admissionDiagnostic)
                .sorted(Comparator
                        .comparing(SchemaDiagnosticData::instanceLocation)
                        .thenComparing(SchemaDiagnosticData::keyword)
                        .thenComparing(SchemaDiagnosticData::machineStableDetail)
                        .thenComparing(SchemaDiagnosticData::stableCode))
                .toList();
    }

    private List<ValidationMessage> validateMessages(JsonNode document) {
        return schema.validate(document).stream().toList();
    }

    private static ScenarioManifestSchemaValidationResult.Diagnostic legacyDiagnostic(
            String repositoryRelativePath,
            ValidationMessage message
    ) {
        return new ScenarioManifestSchemaValidationResult.Diagnostic(
                ScenarioManifestSchemaValidationResult.Code.SCHEMA_VIOLATION,
                repositoryRelativePath,
                String.valueOf(message.getInstanceLocation()),
                message.getType(),
                message.getMessage());
    }

    private static SchemaDiagnosticData admissionDiagnostic(ValidationMessage message) {
        String detail = message.getSchemaLocation() + "|" + message.getCode();
        return new SchemaDiagnosticData(
                "SCHEMA_VIOLATION",
                jsonPointer(message),
                message.getType(),
                detail,
                message.getMessage());
    }

    private static String jsonPointer(ValidationMessage message) {
        var path = message.getInstanceLocation();
        StringBuilder pointer = new StringBuilder();
        for (int index = 0; index < path.getNameCount(); index++) {
            String token = String.valueOf(path.getElement(index));
            pointer.append('/').append(token.replace("~", "~0").replace("/", "~1"));
        }
        return pointer.toString();
    }

    private static JsonSchema loadSchema() {
        try (InputStream input = ScenarioManifestSchemaValidator.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (input == null) throw new IllegalStateException("Scenario Authority schema is unavailable");
            JsonNode schemaNode = new ObjectMapper().readTree(input);
            return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(schemaNode);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load Scenario Authority schema", exception);
        }
    }

    record SchemaDiagnosticData(
            String stableCode,
            String instanceLocation,
            String keyword,
            String machineStableDetail,
            String humanMessage
    ) {
        SchemaDiagnosticData {
            stableCode = requireNonBlank(stableCode, "stableCode");
            Objects.requireNonNull(instanceLocation, "instanceLocation");
            keyword = requireNonBlank(keyword, "keyword");
            machineStableDetail = requireNonBlank(machineStableDetail, "machineStableDetail");
            humanMessage = requireNonBlank(humanMessage, "humanMessage");
        }

        private static String requireNonBlank(String value, String field) {
            Objects.requireNonNull(value, field);
            if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
            return value;
        }
    }
}
