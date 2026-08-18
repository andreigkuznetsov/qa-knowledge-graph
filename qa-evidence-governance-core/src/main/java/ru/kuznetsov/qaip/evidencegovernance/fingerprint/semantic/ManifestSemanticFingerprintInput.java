package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import java.util.List;
import java.util.Objects;

/** Immutable ADR-015 admitted Manifest semantic fingerprint input. */
public record ManifestSemanticFingerprintInput(
        String manifestSemanticCanonicalizationVersion,
        String claimedAuthority,
        String manifestFormatIdentifier,
        String schemaVersion,
        String scenarioIdentitySchemeVersion,
        String sourceNormalizationVersion,
        List<ScenarioSemanticFingerprint> scenarioSemanticFingerprints
) {
    public ManifestSemanticFingerprintInput {
        requireExact(manifestSemanticCanonicalizationVersion,
                ManifestSemanticFingerprintEncoder.ENCODING_IDENTIFIER,
                "manifestSemanticCanonicalizationVersion");
        claimedAuthority = requireNonEmpty(claimedAuthority, "claimedAuthority");
        requireExact(manifestFormatIdentifier,
                ManifestSemanticFingerprintEncoder.MANIFEST_FORMAT_IDENTIFIER,
                "manifestFormatIdentifier");
        requireExact(schemaVersion, ManifestSemanticFingerprintEncoder.SCHEMA_VERSION,
                "schemaVersion");
        requireExact(scenarioIdentitySchemeVersion,
                ManifestSemanticFingerprintEncoder.SCENARIO_IDENTITY_SCHEME_VERSION,
                "scenarioIdentitySchemeVersion");
        requireExact(sourceNormalizationVersion,
                ManifestSemanticFingerprintEncoder.SOURCE_NORMALIZATION_VERSION,
                "sourceNormalizationVersion");
        scenarioSemanticFingerprints = List.copyOf(
                Objects.requireNonNull(scenarioSemanticFingerprints, "scenarioSemanticFingerprints"));
    }

    private static String requireNonEmpty(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isEmpty()) throw new IllegalArgumentException(field + " must not be empty");
        return value;
    }

    private static void requireExact(String actual, String expected, String field) {
        Objects.requireNonNull(actual, field);
        if (!expected.equals(actual)) throw new IllegalArgumentException(field + " must be " + expected);
    }
}
