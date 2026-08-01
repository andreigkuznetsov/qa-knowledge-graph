package ru.kuznetsov.qaip.core.importing.parsing;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

/**
 * Logically immutable, opaque owner of one parsed JSON value. It is a parsing
 * proof only, makes no schema-validity claim, and exposes no public Jackson API.
 */
public final class ParsedProjectDocument {
    private final JsonNode value;

    ParsedProjectDocument(JsonNode value) {
        this.value = Objects.requireNonNull(value, "value").deepCopy();
    }

    JsonNode internalJsonTreeCopy() {
        return value.deepCopy();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof ParsedProjectDocument that && value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
