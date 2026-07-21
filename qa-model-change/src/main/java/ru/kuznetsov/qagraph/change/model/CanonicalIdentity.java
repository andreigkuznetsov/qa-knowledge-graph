package ru.kuznetsov.qagraph.change.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Exact Canonical QA Model v0.1 node or relationship identity.
 */
public record CanonicalIdentity(String value) {

    private static final int MAX_LENGTH = 120;
    private static final Pattern IDENTIFIER = Pattern.compile(
            "^[A-Za-z0-9][A-Za-z0-9._:-]*$"
    );

    public CanonicalIdentity {
        Objects.requireNonNull(value, "value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
        if (value.length() > MAX_LENGTH || !IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "value must satisfy Canonical QA Model v0.1 identifier constraints"
            );
        }
    }
}
