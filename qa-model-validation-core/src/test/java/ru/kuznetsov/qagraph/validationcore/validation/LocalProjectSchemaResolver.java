package ru.kuznetsov.qagraph.validationcore.validation;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SpecVersion;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

final class LocalProjectSchemaResolver {
    static final String QA_MODEL = "https://example.local/schemas/qa-model-v0.1.schema.json";
    static final String DECLARED_CHANGES = "https://example.local/schemas/qaip-declared-changes-v1.schema.json";
    static final String MANIFEST = "https://example.local/schemas/impact-evidence-manifest-v1.schema.json";
    static final String CONTEXT = "https://example.local/schemas/impact-analysis-context-v1.schema.json";
    static final String SUBJECT = "https://example.local/schemas/qaip-project-subject-candidate-v1.schema.json";
    static final String PROJECT = "https://example.local/schemas/qaip-project-v1.schema.json";

    private static final Map<String, String> RESOURCES = Map.of(
            QA_MODEL, "/schemas/qa-model-v0.1.schema.json",
            DECLARED_CHANGES, "/schemas/qaip-declared-changes-v1.schema.json",
            MANIFEST, "/schemas/impact-evidence-manifest-v1.schema.json",
            CONTEXT, "/schemas/impact-analysis-context-v1.schema.json",
            SUBJECT, "/schemas/qaip-project-subject-candidate-v1.schema.json",
            PROJECT, "/schemas/qaip-project-v1.schema.json");

    private final Map<String, String> resources;
    private final JsonSchemaFactory factory;

    LocalProjectSchemaResolver() {
        this(RESOURCES);
    }

    LocalProjectSchemaResolver(String omittedUri) {
        this(RESOURCES.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(omittedUri))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    LocalProjectSchemaResolver(Map<String, String> resources) {
        this.resources = Map.copyOf(resources);
        this.factory = JsonSchemaFactory.getInstance(
                SpecVersion.VersionFlag.V202012,
                builder -> builder.schemaLoaders(loaders -> loaders.schemas(this::schemaText)));
    }

    JsonSchema resolve(String absoluteUri) {
        if (!resources.containsKey(withoutFragment(absoluteUri))) {
            throw new IllegalArgumentException("Unknown schema URI: " + absoluteUri);
        }
        return factory.getSchema(SchemaLocation.of(absoluteUri));
    }

    private String schemaText(String absoluteUri) {
        String resource = resources.get(withoutFragment(absoluteUri));
        if (resource == null) {
            throw new IllegalArgumentException("Unknown schema URI: " + absoluteUri);
        }
        try (InputStream input = LocalProjectSchemaResolver.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("Missing schema resource: " + resource);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot read schema resource: " + resource, exception);
        }
    }

    private static String withoutFragment(String value) {
        URI uri = URI.create(value);
        return URI.create(uri.getScheme() + "://" + uri.getAuthority() + uri.getPath()).toString();
    }
}
