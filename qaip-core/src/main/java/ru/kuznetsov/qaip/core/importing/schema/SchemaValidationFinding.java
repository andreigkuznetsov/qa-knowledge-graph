package ru.kuznetsov.qaip.core.importing.schema;

import ru.kuznetsov.qaip.core.importing.parsing.JsonInstanceLocation;

import java.util.Objects;

/**
 * Library-independent schema diagnostic with an RFC 6901 instance location,
 * exact schema location, violated keyword and canonical QAIP message.
 */
public record SchemaValidationFinding(
        SchemaValidationFindingCode code,
        JsonInstanceLocation instanceLocation,
        JsonSchemaLocation schemaLocation,
        String keyword,
        String message) {
    public SchemaValidationFinding {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(instanceLocation, "instanceLocation");
        Objects.requireNonNull(schemaLocation, "schemaLocation");
        keyword = requireNonBlank(keyword, "keyword");
        message = requireNonBlank(message, "message");
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
