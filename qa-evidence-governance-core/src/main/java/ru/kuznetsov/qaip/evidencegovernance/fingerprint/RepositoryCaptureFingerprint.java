package ru.kuznetsov.qaip.evidencegovernance.fingerprint;

import java.util.Objects;
import java.util.regex.Pattern;

/** Immutable ADR-013 Repository Capture Snapshot content fingerprint. */
public record RepositoryCaptureFingerprint(String value) {
    public static final String VALUE_IDENTIFIER = "scenario-authority-repository-capture-v1";
    public static final String VALUE_PREFIX = VALUE_IDENTIFIER + ":";

    private static final Pattern VALUE_PATTERN = Pattern.compile(
            "^" + VALUE_IDENTIFIER + ":[0-9a-f]{64}$");

    public RepositoryCaptureFingerprint {
        Objects.requireNonNull(value, "value must not be null");
        if (!VALUE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("value must be " + VALUE_PREFIX
                    + " followed by 64 lowercase hexadecimal characters");
        }
    }

    /** Returns the lowercase hexadecimal digest without its fingerprint-domain prefix. */
    public String digestHex() {
        return value.substring(VALUE_PREFIX.length());
    }
}
