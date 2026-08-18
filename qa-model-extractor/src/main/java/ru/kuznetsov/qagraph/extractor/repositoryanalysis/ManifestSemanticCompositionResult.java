package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.ManifestSemanticFingerprint;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.ManifestSemanticFingerprintInput;

import java.util.Objects;

/** Immutable accepted Manifest semantic composition and authoritative fingerprint. */
public record ManifestSemanticCompositionResult(
        ManifestSemanticFingerprintInput acceptedInput,
        ManifestSemanticFingerprint fingerprint
) {
    public ManifestSemanticCompositionResult {
        Objects.requireNonNull(acceptedInput, "acceptedInput");
        Objects.requireNonNull(fingerprint, "fingerprint");
    }
}
