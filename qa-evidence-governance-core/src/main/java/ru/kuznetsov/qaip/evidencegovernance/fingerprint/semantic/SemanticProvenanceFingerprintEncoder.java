package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import ru.kuznetsov.qaip.evidencegovernance.canonical.CanonicalBinaryWriter;
import ru.kuznetsov.qaip.evidencegovernance.canonical.CanonicalSha256;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RawSourceMemberFingerprint;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprint;

import java.util.Objects;

/** Authoritative encoder for the first active Scenario semantic-provenance activity. */
public final class SemanticProvenanceFingerprintEncoder {
    public static final String DOMAIN =
            "QAIP\u0000SCENARIO_AUTHORITY_SEMANTIC_PROVENANCE\u0000V1";
    public static final String ENCODING_IDENTIFIER =
            "scenario-authority-semantic-provenance-c14n-v1";
    public static final String DIGEST_IDENTIFIER = CanonicalSha256.ALGORITHM_IDENTIFIER;

    private SemanticProvenanceFingerprintEncoder() {
    }

    public static byte[] encode(SemanticProvenanceFingerprintInput input) {
        Objects.requireNonNull(input, "input");
        SemanticProvenanceOutputReference output = input.outputReference();
        CanonicalBinaryWriter writer = new CanonicalBinaryWriter()
                .writeDomain(DOMAIN)
                .writeText(ENCODING_IDENTIFIER)
                .writeText(DIGEST_IDENTIFIER)
                .writeText(input.semanticCanonicalizationVersion());
        writeIdentity(writer, input.provenanceIdentity());
        writer
                .writeText(input.activityKind())
                .writeText(input.activityVersion());
        writeOutputIdentity(writer, output.outputIdentity());
        writeOutputReference(writer, output);
        return writer
                .writeOrderedCollection(input.parents(), SemanticProvenanceFingerprintEncoder::writeParent)
                .writeText(input.derivationOutcome())
                .toByteArray();
    }

    public static SemanticProvenanceFingerprint fingerprint(SemanticProvenanceFingerprintInput input) {
        return new SemanticProvenanceFingerprint(SemanticProvenanceFingerprint.VALUE_PREFIX
                + CanonicalSha256.lowercaseHexDigest(encode(input)));
    }

    /** Exact canonical bytes of the stable structured provenance identity. */
    public static byte[] identityBytes(SemanticProvenanceIdentityV1 identity) {
        CanonicalBinaryWriter writer = new CanonicalBinaryWriter();
        writeIdentity(writer, Objects.requireNonNull(identity, "identity"));
        return writer.toByteArray();
    }

    /** Exact canonical bytes of the single supported typed parent reference. */
    public static byte[] parentBytes(SemanticProvenanceParentReference parent) {
        CanonicalBinaryWriter writer = new CanonicalBinaryWriter();
        writeParent(writer, Objects.requireNonNull(parent, "parent"));
        return writer.toByteArray();
    }

    private static void writeIdentity(CanonicalBinaryWriter writer, SemanticProvenanceIdentityV1 identity) {
        writer.writeText(identity.identityContractVersion())
                .writeText(identity.activityKind())
                .writeText(identity.outputKind());
        writeOutputIdentity(writer, identity.outputDatumIdentity());
    }

    private static void writeOutputIdentity(
            CanonicalBinaryWriter writer,
            AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference identity
    ) {
        writer.writeText(SemanticProvenanceIdentityV1.OUTPUT_KIND);
        writeParentMember(writer, identity);
    }

    private static void writeOutputReference(
            CanonicalBinaryWriter writer,
            SemanticProvenanceOutputReference output
    ) {
        writer.writeText(output.outputKind())
                .writeText(AttributedMemberOutcomeFingerprint.VALUE_IDENTIFIER)
                .writeText(output.fingerprint().value());
    }

    private static void writeParent(CanonicalBinaryWriter writer, SemanticProvenanceParentReference parent) {
        writer.writeText(parent.parentKind());
        writeParentMember(writer, parent.capturedMember());
    }

    private static void writeParentMember(
            CanonicalBinaryWriter writer,
            AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference parent
    ) {
        writer.writeText(parent.parentSourceId())
                .writeText(parent.parentSnapshotId())
                .writeText(RepositoryCaptureFingerprint.VALUE_IDENTIFIER)
                .writeText(parent.parentContentFingerprint().value())
                .writeText(parent.normalizedRepositoryRelativePath())
                .writeUnsigned64(parent.rawByteLength())
                .writeText(RawSourceMemberFingerprint.ALGORITHM_IDENTIFIER)
                .writeText(parent.rawMemberFingerprint().value());
    }
}
