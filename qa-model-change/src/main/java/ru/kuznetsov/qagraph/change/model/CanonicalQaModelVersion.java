package ru.kuznetsov.qagraph.change.model;

import java.util.Objects;

/**
 * Exact Canonical QA Model schema version associated with an artifact state.
 */
public record CanonicalQaModelVersion(String value) {

    public static final CanonicalQaModelVersion V0_1 =
            new CanonicalQaModelVersion("0.1");

    public CanonicalQaModelVersion {
        Objects.requireNonNull(value, "value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    public boolean isSupported() {
        return V0_1.value.equals(value);
    }
}
