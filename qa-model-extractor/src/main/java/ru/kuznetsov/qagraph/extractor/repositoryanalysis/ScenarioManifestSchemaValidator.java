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
            List<ScenarioManifestSchemaValidationResult.Diagnostic> diagnostics = schema
                    .validate(member.document()).stream()
                    .map(message -> diagnostic(path, message))
                    .sorted(DIAGNOSTIC_ORDER)
                    .toList();
            members.add(new ScenarioManifestSchemaValidationResult.ValidatedMember(member, diagnostics));
        }
        return new ScenarioManifestSchemaValidationResult(members);
    }

    private static ScenarioManifestSchemaValidationResult.Diagnostic diagnostic(
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

    private static JsonSchema loadSchema() {
        try (InputStream input = ScenarioManifestSchemaValidator.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (input == null) throw new IllegalStateException("Scenario Authority schema is unavailable");
            JsonNode schemaNode = new ObjectMapper().readTree(input);
            return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(schemaNode);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load Scenario Authority schema", exception);
        }
    }
}
