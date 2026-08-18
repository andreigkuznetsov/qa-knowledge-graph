package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import java.util.Objects;

/** Immutable accepted Scenario composition and its authoritative fingerprint. */
public record ScenarioSemanticCompositionResult(
        ScenarioSemanticFingerprintInput acceptedInput,
        ScenarioSemanticFingerprint fingerprint
) {
    public ScenarioSemanticCompositionResult {
        Objects.requireNonNull(acceptedInput, "acceptedInput");
        Objects.requireNonNull(fingerprint, "fingerprint");
    }
}
