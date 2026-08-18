package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import ru.kuznetsov.qaip.evidencegovernance.canonical.CanonicalBinaryWriter;
import ru.kuznetsov.qaip.evidencegovernance.canonical.CanonicalSha256;

import java.util.Objects;

/** Single authoritative ADR-015 admitted Manifest semantic encoder. */
public final class ManifestSemanticFingerprintEncoder {
    public static final String ENCODING_IDENTIFIER =
            "scenario-authority-manifest-semantic-c14n-v1";
    public static final String DIGEST_IDENTIFIER = CanonicalSha256.ALGORITHM_IDENTIFIER;
    public static final String DOMAIN =
            "QAIP\u0000SCENARIO_AUTHORITY_MANIFEST_SEMANTIC\u0000V1";
    public static final String MANIFEST_FORMAT_IDENTIFIER =
            "qaip-scenario-authority-manifest-v1";
    public static final String SCHEMA_VERSION = "1.0";
    public static final String SCENARIO_IDENTITY_SCHEME_VERSION = "qaip-scenario-identity-v1";
    public static final String SOURCE_NORMALIZATION_VERSION =
            "scenario-authority-source-normalization-v1";

    private ManifestSemanticFingerprintEncoder() {
    }

    /** Encodes ordered Scenario fingerprint references without inspecting Scenario content. */
    public static byte[] encode(ManifestSemanticFingerprintInput input) {
        Objects.requireNonNull(input, "input");
        return new CanonicalBinaryWriter()
                .writeDomain(DOMAIN)
                .writeText(ENCODING_IDENTIFIER)
                .writeText(DIGEST_IDENTIFIER)
                .writeText(input.manifestSemanticCanonicalizationVersion())
                .writeText(input.claimedAuthority())
                .writeText(input.manifestFormatIdentifier())
                .writeText(input.schemaVersion())
                .writeText(input.scenarioIdentitySchemeVersion())
                .writeText(input.sourceNormalizationVersion())
                .writeOrderedCollection(input.scenarioSemanticFingerprints(),
                        ManifestSemanticFingerprintEncoder::writeScenarioFingerprintReference)
                .toByteArray();
    }

    public static ManifestSemanticFingerprint fingerprint(ManifestSemanticFingerprintInput input) {
        return new ManifestSemanticFingerprint(ManifestSemanticFingerprint.VALUE_PREFIX
                + CanonicalSha256.lowercaseHexDigest(encode(input)));
    }

    private static void writeScenarioFingerprintReference(
            CanonicalBinaryWriter writer,
            ScenarioSemanticFingerprint fingerprint
    ) {
        writer.writeText(ScenarioSemanticFingerprint.VALUE_IDENTIFIER)
                .writeText(Objects.requireNonNull(fingerprint, "Scenario fingerprint").value());
    }
}
