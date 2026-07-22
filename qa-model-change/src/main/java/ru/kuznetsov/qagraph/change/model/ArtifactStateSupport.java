package ru.kuznetsov.qagraph.change.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

final class ArtifactStateSupport {

    private ArtifactStateSupport() {
    }

    static JsonNode objectCopy(JsonNode snapshot) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        if (!snapshot.isObject()) {
            throw new IllegalArgumentException(
                    "snapshot must be a JSON object"
            );
        }
        return snapshot.deepCopy();
    }

    static CanonicalIdentity identity(JsonNode snapshot) {
        JsonNode id = snapshot.get("id");
        if (id == null || !id.isTextual()) {
            throw new IllegalArgumentException(
                    "snapshot must contain a textual id"
            );
        }
        return new CanonicalIdentity(id.textValue());
    }

    static String requiredText(JsonNode snapshot, String property) {
        JsonNode value = snapshot.get(property);
        if (value == null || !value.isTextual()) {
            throw new IllegalArgumentException(
                    "snapshot must contain textual " + property
            );
        }
        return value.textValue();
    }
}
