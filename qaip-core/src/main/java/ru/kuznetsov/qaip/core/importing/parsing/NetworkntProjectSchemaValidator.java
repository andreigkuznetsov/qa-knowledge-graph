package ru.kuznetsov.qaip.core.importing.parsing;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonNodePath;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaException;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.PathType;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import ru.kuznetsov.qaip.core.importing.schema.JsonSchemaLocation;
import ru.kuznetsov.qaip.core.importing.schema.ProjectSchemaValidationContractException;
import ru.kuznetsov.qaip.core.importing.schema.ProjectSchemaValidator;
import ru.kuznetsov.qaip.core.importing.schema.SchemaValidationAccepted;
import ru.kuznetsov.qaip.core.importing.schema.SchemaValidationFinding;
import ru.kuznetsov.qaip.core.importing.schema.SchemaValidationFindingCode;
import ru.kuznetsov.qaip.core.importing.schema.SchemaValidationRejected;
import ru.kuznetsov.qaip.core.importing.schema.SchemaValidationResult;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Thread-safe validator backed by one compiled Draft 2020-12 canonical schema. */
public final class NetworkntProjectSchemaValidator implements ProjectSchemaValidator {
    static final String PROJECT_SCHEMA = "https://example.local/schemas/qaip-project-v1.schema.json";
    static final String QA_MODEL_SCHEMA = "https://example.local/schemas/qa-model-v0.1.schema.json";
    static final String DECLARED_CHANGES_SCHEMA = "https://example.local/schemas/qaip-declared-changes-v1.schema.json";
    static final String MANIFEST_SCHEMA = "https://example.local/schemas/impact-evidence-manifest-v1.schema.json";
    static final String CONTEXT_SCHEMA = "https://example.local/schemas/impact-analysis-context-v1.schema.json";
    static final String SUBJECT_SCHEMA = "https://example.local/schemas/qaip-project-subject-candidate-v1.schema.json";

    private static final Map<String, String> CANONICAL_RESOURCES = Map.of(
            PROJECT_SCHEMA, "/schemas/qaip-project-v1.schema.json",
            QA_MODEL_SCHEMA, "/schemas/qa-model-v0.1.schema.json",
            DECLARED_CHANGES_SCHEMA, "/schemas/qaip-declared-changes-v1.schema.json",
            MANIFEST_SCHEMA, "/schemas/impact-evidence-manifest-v1.schema.json",
            CONTEXT_SCHEMA, "/schemas/impact-analysis-context-v1.schema.json",
            SUBJECT_SCHEMA, "/schemas/qaip-project-subject-candidate-v1.schema.json");

    private final JsonSchema schema;

    public NetworkntProjectSchemaValidator() {
        this(CANONICAL_RESOURCES);
    }

    NetworkntProjectSchemaValidator(Map<String, String> resources) {
        Objects.requireNonNull(resources, "resources");
        try {
            SchemaValidatorsConfig config = SchemaValidatorsConfig.builder()
                    .failFast(false)
                    .formatAssertionsEnabled(false)
                    .pathType(PathType.JSON_POINTER)
                    .preloadJsonSchema(true)
                    .typeLoose(false)
                    .build();
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(
                    SpecVersion.VersionFlag.V202012,
                    builder -> builder.schemaLoaders(loaders -> loaders.schemas(uri -> schemaText(resources, uri))));
            this.schema = factory.getSchema(SchemaLocation.of(PROJECT_SCHEMA), config);
        } catch (SchemaResourceLoadingException | JsonSchemaException exception) {
            throw new ProjectSchemaValidationContractException(
                    "Cannot initialize the canonical QAIP Project JSON Schema validator", exception);
        }
    }

    @Override
    public SchemaValidationResult validate(ParsedProjectDocument document) {
        Objects.requireNonNull(document, "document");
        JsonNode tree = document.internalJsonTreeCopy();
        final List<SchemaValidationFinding> findings;
        try {
            findings = schema.validate(tree).stream().map(NetworkntProjectSchemaValidator::mapFinding).toList();
        } catch (SchemaResourceLoadingException | JsonSchemaException exception) {
            throw new ProjectSchemaValidationContractException(
                    "Canonical QAIP Project JSON Schema validation failed internally", exception);
        }
        if (!findings.isEmpty()) {
            return new SchemaValidationRejected(findings);
        }
        return new SchemaValidationAccepted(new SchemaValidProjectDocument(tree));
    }

    private static SchemaValidationFinding mapFinding(ValidationMessage message) {
        String keyword = message.getType();
        return new SchemaValidationFinding(
                SchemaValidationFindingCode.SCHEMA_VIOLATION,
                new JsonInstanceLocation(toJsonPointer(message.getInstanceLocation())),
                new JsonSchemaLocation(message.getSchemaLocation().toString()),
                keyword,
                canonicalMessage(keyword));
    }

    private static String toJsonPointer(JsonNodePath path) {
        StringBuilder pointer = new StringBuilder();
        for (int index = 0; index < path.getNameCount(); index++) {
            Object segment = path.getElement(index);
            pointer.append('/').append(escapePointerSegment(String.valueOf(segment)));
        }
        return pointer.toString();
    }

    private static String escapePointerSegment(String segment) {
        return segment.replace("~", "~0").replace("/", "~1");
    }

    private static String canonicalMessage(String keyword) {
        return switch (keyword) {
            case "required" -> "Required property is missing.";
            case "type" -> "Value has an invalid JSON type.";
            case "enum", "const" -> "Value is not allowed by the schema.";
            case "additionalProperties" -> "Property is not allowed by the schema.";
            default -> "Value violates the JSON Schema keyword '" + keyword + "'.";
        };
    }

    private static String schemaText(Map<String, String> resources, String absoluteUri) {
        String resource = resources.get(withoutFragment(absoluteUri));
        if (resource == null) {
            throw new SchemaResourceLoadingException("No local schema mapping for required URI", null);
        }
        try (InputStream input = NetworkntProjectSchemaValidator.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new SchemaResourceLoadingException("Canonical schema resource is missing", null);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new SchemaResourceLoadingException("Cannot read canonical schema resource", exception);
        }
    }

    private static String withoutFragment(String value) {
        URI uri = URI.create(value);
        return URI.create(uri.getScheme() + "://" + uri.getAuthority() + uri.getPath()).toString();
    }

    private static final class SchemaResourceLoadingException extends RuntimeException {
        private SchemaResourceLoadingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
