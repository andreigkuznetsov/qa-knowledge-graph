package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

public final class ScenarioManifestSchemaValidator {
    public static final String SCHEMA_CONTRACT_IDENTIFIER =
            "qaip-scenario-authority-manifest-schema-v1";
    public static final String SCHEMA_RESOURCE =
            "/schemas/qaip-scenario-authority-manifest-v1.schema.json";
    public static final String EXPECTED_SCHEMA_SHA256 =
            "7fdac4321c125cadec4afe734460920afc3dfcaf6ea8bb1f49cee43b49a901f1";
    public static final String SCHEMA_CONTENT_IDENTITY = "sha256:" + EXPECTED_SCHEMA_SHA256;

    private static final Comparator<ScenarioManifestSchemaValidationResult.Diagnostic> DIAGNOSTIC_ORDER =
            Comparator.comparing(ScenarioManifestSchemaValidationResult.Diagnostic::instanceLocation)
                    .thenComparing(ScenarioManifestSchemaValidationResult.Diagnostic::keyword)
                    .thenComparing(ScenarioManifestSchemaValidationResult.Diagnostic::message);

    private final JsonSchema schema;
    private final String schemaContentIdentity;

    public ScenarioManifestSchemaValidator() {
        this(loadSchemaBytes());
    }

    ScenarioManifestSchemaValidator(byte[] schemaBytes) {
        if (schemaBytes == null) {
            throw new ScenarioSchemaDiagnosticMappingException(
                    "Scenario Authority V1 schema bytes are missing");
        }
        byte[] immutableBytes = schemaBytes.clone();
        String actualFingerprint = sha256(immutableBytes);
        if (!EXPECTED_SCHEMA_SHA256.equals(actualFingerprint)) {
            throw new ScenarioSchemaDiagnosticMappingException(
                    "Scenario Authority V1 schema content identity mismatch");
        }
        this.schema = parseSchema(immutableBytes);
        this.schemaContentIdentity = "sha256:" + actualFingerprint;
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

    private List<ValidationMessage> validateMessages(JsonNode document) {
        try {
            return schema.validate(document).stream().toList();
        } catch (ScenarioSchemaDiagnosticMappingException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ScenarioSchemaDiagnosticMappingException(
                    "Scenario Authority V1 schema validation failed", exception);
        }
    }

    List<ValidationMessage> validationMessages(JsonNode document) {
        Objects.requireNonNull(document, "document");
        return validateMessages(document);
    }

    String schemaContentIdentity() {
        return schemaContentIdentity;
    }

    /** Produces legacy noncanonical presentation data for the pre-ADR-014 compatibility API only. */
    @SuppressWarnings("deprecation")
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

    private static byte[] loadSchemaBytes() {
        try (InputStream input = ScenarioManifestSchemaValidator.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (input == null) {
                throw new ScenarioSchemaDiagnosticMappingException(
                        "Scenario Authority V1 schema resource is unavailable");
            }
            return input.readAllBytes();
        } catch (IOException exception) {
            throw new ScenarioSchemaDiagnosticMappingException(
                    "Cannot load Scenario Authority V1 schema resource", exception);
        } catch (ScenarioSchemaDiagnosticMappingException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ScenarioSchemaDiagnosticMappingException(
                    "Cannot access Scenario Authority V1 schema resource", exception);
        }
    }

    private static JsonSchema parseSchema(byte[] schemaBytes) {
        try {
            JsonNode schemaNode = new ObjectMapper().readTree(schemaBytes);
            if (schemaNode == null) {
                throw new ScenarioSchemaDiagnosticMappingException(
                        "Scenario Authority V1 schema resource is empty");
            }
            return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(schemaNode);
        } catch (ScenarioSchemaDiagnosticMappingException exception) {
            throw exception;
        } catch (RuntimeException | IOException exception) {
            throw new ScenarioSchemaDiagnosticMappingException(
                    "Cannot parse or resolve Scenario Authority V1 schema", exception);
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new ScenarioSchemaDiagnosticMappingException("SHA-256 is unavailable", exception);
        }
    }

}
