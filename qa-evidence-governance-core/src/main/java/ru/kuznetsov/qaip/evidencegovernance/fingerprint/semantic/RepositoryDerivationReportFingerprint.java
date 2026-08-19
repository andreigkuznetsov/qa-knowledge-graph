package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import java.util.Objects;
import java.util.regex.Pattern;

/** Immutable ADR-015 Repository Derivation Report fingerprint. */
public record RepositoryDerivationReportFingerprint(String value) {
    public static final String VALUE_IDENTIFIER = "scenario-authority-repository-derivation-v1";
    public static final String VALUE_PREFIX = VALUE_IDENTIFIER + ":";
    private static final Pattern VALUE_PATTERN = Pattern.compile(
            "^" + VALUE_IDENTIFIER + ":[0-9a-f]{64}$");

    public RepositoryDerivationReportFingerprint {
        Objects.requireNonNull(value, "value");
        if (!VALUE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "value must be " + VALUE_PREFIX + " followed by 64 lowercase hexadecimal characters");
        }
    }
}
