package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import java.util.Objects;
import java.util.regex.Pattern;

/** Immutable ADR-015 unresolved Business Rule-reference semantic fingerprint. */
public record BusinessRuleReferenceSemanticFingerprint(String value) {
    public static final String VALUE_IDENTIFIER =
            "scenario-authority-business-rule-reference-semantic-v1";
    public static final String VALUE_PREFIX = VALUE_IDENTIFIER + ":";
    private static final Pattern VALUE_PATTERN = Pattern.compile(
            "^" + VALUE_IDENTIFIER + ":[0-9a-f]{64}$");

    public BusinessRuleReferenceSemanticFingerprint {
        Objects.requireNonNull(value, "value");
        if (!VALUE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "value must be " + VALUE_PREFIX + " followed by 64 lowercase hexadecimal characters");
        }
    }

    public String digestHex() {
        return value.substring(VALUE_PREFIX.length());
    }
}
