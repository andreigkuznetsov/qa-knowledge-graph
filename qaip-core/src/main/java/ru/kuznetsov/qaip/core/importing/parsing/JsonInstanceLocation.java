package ru.kuznetsov.qaip.core.importing.parsing;

import java.util.Objects;

/** RFC 6901 JSON Pointer; the empty string denotes the document root. */
public record JsonInstanceLocation(String value) {
    public static final JsonInstanceLocation ROOT = new JsonInstanceLocation("");

    public JsonInstanceLocation {
        Objects.requireNonNull(value, "value");
        if (!value.isEmpty() && !value.startsWith("/")) {
            throw new IllegalArgumentException("JSON Pointer must be empty or start with '/'");
        }
        for (int index = 0; index < value.length(); index++) {
            if (value.charAt(index) == '~'
                    && (index + 1 == value.length()
                    || (value.charAt(index + 1) != '0' && value.charAt(index + 1) != '1'))) {
                throw new IllegalArgumentException("JSON Pointer contains an invalid escape");
            }
        }
    }
}
