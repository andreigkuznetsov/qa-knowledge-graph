package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qagraph.validationcore.scenarioauthority.ScenarioAuthorityManifestSchemaValidationV1;
import ru.kuznetsov.qagraph.validationcore.scenarioauthority.ScenarioAuthorityManifestSchemaValidatorV1;
import ru.kuznetsov.qagraph.validationcore.scenarioauthority.ScenarioAuthoritySchemaValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Extractor compatibility projection over the sole Validation Core V1 validator. */
public final class ScenarioManifestSchemaValidator {
    public static final String SCHEMA_CONTRACT_IDENTIFIER =
            ScenarioAuthorityManifestSchemaValidatorV1.SCHEMA_CONTRACT_IDENTIFIER;
    public static final String SCHEMA_RESOURCE = ScenarioAuthorityManifestSchemaValidatorV1.SCHEMA_RESOURCE;
    public static final String EXPECTED_SCHEMA_SHA256 =
            ScenarioAuthorityManifestSchemaValidatorV1.EXPECTED_SCHEMA_SHA256;
    public static final String SCHEMA_CONTENT_IDENTITY =
            ScenarioAuthorityManifestSchemaValidatorV1.SCHEMA_CONTENT_IDENTITY;

    private final ScenarioAuthorityManifestSchemaValidatorV1 delegate;

    public ScenarioManifestSchemaValidator() {
        delegate = new ScenarioAuthorityManifestSchemaValidatorV1();
    }

    public ScenarioManifestSchemaValidationResult validate(ScenarioManifestJsonParseResult.Completed parsed) {
        Objects.requireNonNull(parsed, "parsed");
        List<ScenarioManifestSchemaValidationResult.ValidatedMember> members = new ArrayList<>();
        for (ScenarioManifestJsonParseResult.ParsedMember member : parsed.members()) {
            List<ScenarioManifestSchemaValidationResult.Diagnostic> diagnostics = validationSignals(member.document())
                    .stream().map(signal -> legacyDiagnostic(member.source().repositoryRelativePath(), signal)).toList();
            members.add(new ScenarioManifestSchemaValidationResult.ValidatedMember(member, diagnostics));
        }
        return new ScenarioManifestSchemaValidationResult(members);
    }

    List<ScenarioAuthorityManifestSchemaValidationV1.Signal> validationSignals(JsonNode document) {
        try {
            return delegate.validate(SCHEMA_CONTRACT_IDENTIFIER, Objects.requireNonNull(document)).signals();
        } catch (ScenarioAuthoritySchemaValidationException failure) {
            throw new ScenarioSchemaDiagnosticMappingException("Scenario Authority V1 schema validation failed", failure);
        }
    }

    String schemaContentIdentity() { return SCHEMA_CONTENT_IDENTITY; }

    @SuppressWarnings("deprecation")
    private static ScenarioManifestSchemaValidationResult.Diagnostic legacyDiagnostic(
            String path, ScenarioAuthorityManifestSchemaValidationV1.Signal signal) {
        return new ScenarioManifestSchemaValidationResult.Diagnostic(
                ScenarioManifestSchemaValidationResult.Code.SCHEMA_VIOLATION,
                path, signal.instancePointer(), signal.keyword(), signal.message());
    }
}
