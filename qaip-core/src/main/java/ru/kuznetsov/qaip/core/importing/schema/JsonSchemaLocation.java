package ru.kuznetsov.qaip.core.importing.schema;

import java.util.Objects;

/** Exact absolute schema URI and optional fragment reported for a violated rule. */
public record JsonSchemaLocation(String value) {
    public JsonSchemaLocation {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }
}
