package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import ru.kuznetsov.qaip.evidencegovernance.canonical.CanonicalBinaryWriter;
import ru.kuznetsov.qaip.evidencegovernance.canonical.CanonicalSha256;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RawSourceMemberFingerprint;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprint;

import java.math.BigInteger;
import java.util.Objects;

/** Single authoritative ADR-015 Repository Derivation Report encoder. */
public final class RepositoryDerivationReportFingerprintEncoder {
    public static final String DOMAIN =
            "QAIP\u0000SCENARIO_AUTHORITY_REPOSITORY_DERIVATION\u0000V1";
    public static final String ENCODING_IDENTIFIER =
            "scenario-authority-repository-derivation-c14n-v1";
    public static final String DIGEST_IDENTIFIER = CanonicalSha256.ALGORITHM_IDENTIFIER;

    private RepositoryDerivationReportFingerprintEncoder() {
    }

    public static byte[] encode(RepositoryDerivationReportFingerprintInput input) {
        Objects.requireNonNull(input, "input");
        var parent = input.parentCapture();
        CanonicalBinaryWriter writer = new CanonicalBinaryWriter()
                .writeDomain(DOMAIN)
                .writeText(ENCODING_IDENTIFIER)
                .writeText(DIGEST_IDENTIFIER)
                .writeText(input.reportContractVersion())
                .writeText(parent.sourceId())
                .writeText(parent.snapshotId());
        writeCaptureFingerprint(writer, parent.contentFingerprint());
        writeCaptureFingerprint(writer, parent.contentFingerprint());
        writer.writeText(input.parserContractIdentifier())
                .writeText(input.attributionContractIdentifier())
                .writeText(input.structuralLocationContractIdentifier())
                .writeUnsigned64(BigInteger.valueOf(parent.regularMembers().size()));
        input.memberOutcomes().forEach(outcome -> writeMemberOutcome(writer, outcome));
        writer.writeUnsigned64(BigInteger.valueOf(input.unsupportedMatchingEntries().size()));
        input.unsupportedMatchingEntries().forEach(entry -> writer
                .writeText(entry.normalizedRepositoryRelativePath())
                .writeText(entry.entryKind())
                .writeText(entry.stableDiagnosticCode()));
        writer.writeOrderedCollection(input.provenance(), (target, attestation) -> target
                .writeText(SemanticProvenanceFingerprint.VALUE_IDENTIFIER)
                .writeText(attestation.fingerprint().value()));
        return writer.toByteArray();
    }

    public static RepositoryDerivationReportFingerprint fingerprint(
            RepositoryDerivationReportFingerprintInput input
    ) {
        return new RepositoryDerivationReportFingerprint(
                RepositoryDerivationReportFingerprint.VALUE_PREFIX
                        + CanonicalSha256.lowercaseHexDigest(encode(input)));
    }

    private static void writeMemberOutcome(
            CanonicalBinaryWriter writer,
            RepositoryDerivationReportFingerprintInput.MemberOutcome outcome
    ) {
        if (outcome instanceof RepositoryDerivationReportFingerprintInput.AttributedMemberRetained attributed) {
            writer.writeText(RepositoryDerivationReportFingerprintInput.AttributedMemberRetained.TAG);
            writeParentMember(writer, attributed.parentMember());
            writer.writeText(attributed.claimedAuthority())
                    .writeText(AttributedMemberOutcomeFingerprint.VALUE_IDENTIFIER)
                    .writeText(attributed.attributedMemberOutput().fingerprint().value());
            return;
        }
        var unattributable =
                (RepositoryDerivationReportFingerprintInput.UnattributableMemberRetained) outcome;
        writer.writeText(RepositoryDerivationReportFingerprintInput.UnattributableMemberRetained.TAG);
        writeParentMember(writer, unattributable.parentMember());
        writer.writeText(unattributable.parseOutcome())
                .writeText(unattributable.attributionOutcome())
                .writeOptional(unattributable.structuralLocation(),
                        (target, location) -> target.writeText(location));
    }

    private static void writeParentMember(
            CanonicalBinaryWriter writer,
            AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference member
    ) {
        writer.writeText(member.parentSourceId())
                .writeText(member.parentSnapshotId());
        writeCaptureFingerprint(writer, member.parentContentFingerprint());
        writer.writeText(member.normalizedRepositoryRelativePath())
                .writeUnsigned64(member.rawByteLength())
                .writeText(RawSourceMemberFingerprint.ALGORITHM_IDENTIFIER)
                .writeText(member.rawMemberFingerprint().value());
    }

    private static void writeCaptureFingerprint(
            CanonicalBinaryWriter writer,
            RepositoryCaptureFingerprint fingerprint
    ) {
        writer.writeText(RepositoryCaptureFingerprint.VALUE_IDENTIFIER)
                .writeText(fingerprint.value());
    }
}
