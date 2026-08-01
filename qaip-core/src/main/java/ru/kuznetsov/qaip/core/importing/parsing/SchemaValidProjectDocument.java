package ru.kuznetsov.qaip.core.importing.parsing;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

/**
 * Logically immutable opaque proof of schema conformance. It makes no binding,
 * application-validity or domain-validity guarantee and exposes no public tree API.
 */
public final class SchemaValidProjectDocument {
    private final JsonNode value;

    SchemaValidProjectDocument(JsonNode value) {
        this.value = Objects.requireNonNull(value, "value").deepCopy();
    }

    JsonNode internalJsonTreeCopy() {
        return value.deepCopy();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof SchemaValidProjectDocument that && value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
