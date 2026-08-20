package ru.kuznetsov.qagraph.validationcore.scenarioauthority;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonNodePath;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/** Sole content-pinned deterministic structural validator for Scenario Authority Manifest V1. */
public final class ScenarioAuthorityManifestSchemaValidatorV1 {
    public static final String SCHEMA_CONTRACT_IDENTIFIER = "qaip-scenario-authority-manifest-schema-v1";
    public static final String SCHEMA_RESOURCE = "/schemas/qaip-scenario-authority-manifest-v1.schema.json";
    public static final String EXPECTED_SCHEMA_SHA256 =
            "7fdac4321c125cadec4afe734460920afc3dfcaf6ea8bb1f49cee43b49a901f1";
    public static final String SCHEMA_CONTENT_IDENTITY = "sha256:" + EXPECTED_SCHEMA_SHA256;

    private static final Comparator<ScenarioAuthorityManifestSchemaValidationV1.Signal> SIGNAL_ORDER =
            Comparator.comparing(ScenarioAuthorityManifestSchemaValidationV1.Signal::instancePointer)
                    .thenComparing(ScenarioAuthorityManifestSchemaValidationV1.Signal::schemaPointer)
                    .thenComparing(ScenarioAuthorityManifestSchemaValidationV1.Signal::keyword)
                    .thenComparing(ScenarioAuthorityManifestSchemaValidationV1.Signal::message);

    private final JsonSchema schema;

    public ScenarioAuthorityManifestSchemaValidatorV1() { this(loadPinnedSchemaBytes()); }

    ScenarioAuthorityManifestSchemaValidatorV1(byte[] schemaBytes) {
        if (schemaBytes == null) fail("Scenario Authority V1 schema bytes are missing");
        byte[] immutable = schemaBytes.clone();
        if (!EXPECTED_SCHEMA_SHA256.equals(sha256(immutable)))
            fail("Scenario Authority V1 schema content identity mismatch");
        schema = parseSchema(immutable);
    }

    public ScenarioAuthorityManifestSchemaValidationV1 validate(
            String schemaContractIdentifier, JsonNode parsedDocument) {
        if (!SCHEMA_CONTRACT_IDENTIFIER.equals(schemaContractIdentifier))
            throw new IllegalArgumentException("unsupported Scenario Authority Manifest schema contract");
        Objects.requireNonNull(parsedDocument, "parsedDocument");
        try {
            List<ScenarioAuthorityManifestSchemaValidationV1.Signal> signals = schema.validate(parsedDocument)
                    .stream().map(ScenarioAuthorityManifestSchemaValidatorV1::signal)
                    .sorted(SIGNAL_ORDER).toList();
            return new ScenarioAuthorityManifestSchemaValidationV1(
                    SCHEMA_CONTRACT_IDENTIFIER, SCHEMA_CONTENT_IDENTITY, signals);
        } catch (ScenarioAuthoritySchemaValidationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ScenarioAuthoritySchemaValidationException(
                    "Scenario Authority V1 schema validation failed", exception);
        }
    }

    private static ScenarioAuthorityManifestSchemaValidationV1.Signal signal(ValidationMessage message) {
        if (message == null || message.getType() == null || message.getSchemaLocation() == null
                || message.getInstanceLocation() == null || message.getMessage() == null)
            fail("validator produced an incomplete structural signal");
        return new ScenarioAuthorityManifestSchemaValidationV1.Signal(message.getType(),
                pointer(message.getSchemaLocation().getFragment()), pointer(message.getInstanceLocation()),
                message.getMessage());
    }

    private static String pointer(JsonNodePath path) {
        if (path == null) fail("validator path is missing");
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < path.getNameCount(); index++) {
            String token = String.valueOf(path.getElement(index));
            result.append('/').append(token.replace("~", "~0").replace("/", "~1"));
        }
        return result.toString();
    }

    private static byte[] loadPinnedSchemaBytes() {
        try (InputStream input = ScenarioAuthorityManifestSchemaValidatorV1.class
                .getResourceAsStream(SCHEMA_RESOURCE)) {
            if (input == null) fail("Scenario Authority V1 schema resource is unavailable");
            return input.readAllBytes();
        } catch (IOException exception) {
            throw new ScenarioAuthoritySchemaValidationException("Cannot load pinned Manifest V1 schema", exception);
        }
    }

    private static JsonSchema parseSchema(byte[] bytes) {
        try {
            JsonNode node = new ObjectMapper().readTree(bytes);
            if (node == null) fail("Scenario Authority V1 schema resource is empty");
            return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(node);
        } catch (ScenarioAuthoritySchemaValidationException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new ScenarioAuthoritySchemaValidationException("Cannot parse pinned Manifest V1 schema", exception);
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new ScenarioAuthoritySchemaValidationException("SHA-256 is unavailable", exception);
        }
    }

    private static void fail(String message) { throw new ScenarioAuthoritySchemaValidationException(message); }
}
