package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import ru.kuznetsov.qaip.evidencegovernance.canonical.CanonicalBinaryWriter;
import ru.kuznetsov.qaip.evidencegovernance.canonical.CanonicalSha256;
import ru.kuznetsov.qaip.evidencegovernance.diagnostic.ScenarioSchemaDiagnostic;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RawSourceMemberFingerprint;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprint;

import java.util.Objects;

/** Single authoritative ADR-015 attributed-member outcome encoder. */
public final class AttributedMemberOutcomeFingerprintEncoder {
    public static final String ENCODING_IDENTIFIER =
            "scenario-authority-attributed-member-outcome-c14n-v1";
    public static final String DIGEST_IDENTIFIER = CanonicalSha256.ALGORITHM_IDENTIFIER;
    public static final String DOMAIN =
            "QAIP\u0000SCENARIO_AUTHORITY_ATTRIBUTED_MEMBER_OUTCOME\u0000V1";

    private AttributedMemberOutcomeFingerprintEncoder() {
    }

    /** Encodes the complete validated input in exact ADR-015 field order. */
    public static byte[] encode(AttributedMemberOutcomeFingerprintInput input) {
        Objects.requireNonNull(input, "input");
        AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference parent =
                input.parentMemberReference();
        return new CanonicalBinaryWriter()
                .writeDomain(DOMAIN)
                .writeText(ENCODING_IDENTIFIER)
                .writeText(DIGEST_IDENTIFIER)
                .writeText(input.attributedMemberSemanticCanonicalizationVersion())
                .writeText(parent.parentSourceId())
                .writeText(parent.parentSnapshotId())
                .writeText(RepositoryCaptureFingerprint.VALUE_IDENTIFIER)
                .writeText(parent.parentContentFingerprint().value())
                .writeText(parent.normalizedRepositoryRelativePath())
                .writeUnsigned64(parent.rawByteLength())
                .writeText(RawSourceMemberFingerprint.ALGORITHM_IDENTIFIER)
                .writeText(parent.rawMemberFingerprint().value())
                .writeText(input.claimedAuthority())
                .writeText(input.parserContractIdentifier())
                .writeText(input.attributionContractIdentifier())
                .writeText(input.parseOutcome().name())
                .writeText(input.attributionOutcome().name())
                .writeOptional(input.attributionStructuralLocation(),
                        (writer, location) -> writer.writeText(location))
                .writeText(input.schemaContractIdentifier())
                .writeText(input.structuralAdmissionState().name())
                .writeOrderedCollection(input.schemaDiagnostics(), ScenarioSchemaDiagnostic::writeCanonical)
                .writeOptional(input.manifestOccurrenceIdentity(),
                        AttributedMemberOutcomeFingerprintEncoder::writeManifestOccurrenceIdentity)
                .writeOptional(input.manifestSemanticFingerprint(),
                        AttributedMemberOutcomeFingerprintEncoder::writeManifestFingerprintReference)
                .toByteArray();
    }

    public static AttributedMemberOutcomeFingerprint fingerprint(
            AttributedMemberOutcomeFingerprintInput input
    ) {
        return new AttributedMemberOutcomeFingerprint(
                AttributedMemberOutcomeFingerprint.VALUE_PREFIX
                        + CanonicalSha256.lowercaseHexDigest(encode(input)));
    }

    private static void writeManifestOccurrenceIdentity(
            CanonicalBinaryWriter writer,
            AttributedMemberOutcomeFingerprintInput.ManifestOccurrenceIdentity identity
    ) {
        writer.writeText(identity.identityVersion())
                .writeText(identity.parentSourceId())
                .writeText(identity.parentSnapshotId())
                .writeText(RepositoryCaptureFingerprint.VALUE_IDENTIFIER)
                .writeText(identity.parentContentFingerprint().value())
                .writeText(identity.normalizedRepositoryRelativePath());
    }

    private static void writeManifestFingerprintReference(
            CanonicalBinaryWriter writer,
            ManifestSemanticFingerprint fingerprint
    ) {
        writer.writeText(ManifestSemanticFingerprint.VALUE_IDENTIFIER)
                .writeText(fingerprint.value());
    }
}
