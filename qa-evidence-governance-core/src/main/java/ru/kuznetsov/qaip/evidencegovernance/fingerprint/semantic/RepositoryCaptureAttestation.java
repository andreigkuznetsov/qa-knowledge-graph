package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprint;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprintEncoder;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprintInput;

import java.util.List;
import java.util.Objects;

/** Positively verified binding between a Repository Capture identity and its exact canonical input. */
public final class RepositoryCaptureAttestation {
    private final String sourceId;
    private final String snapshotId;
    private final RepositoryCaptureFingerprintInput fingerprintInput;
    private final RepositoryCaptureFingerprint contentFingerprint;
    private final List<AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference> regularMembers;
    private final List<RepositoryCaptureFingerprintInput.UnsupportedMatchingEntry> unsupportedMatchingEntries;

    private RepositoryCaptureAttestation(
            String sourceId,
            String snapshotId,
            RepositoryCaptureFingerprintInput fingerprintInput,
            RepositoryCaptureFingerprint contentFingerprint,
            List<AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference> regularMembers,
            List<RepositoryCaptureFingerprintInput.UnsupportedMatchingEntry> unsupportedMatchingEntries
    ) {
        this.sourceId = sourceId;
        this.snapshotId = snapshotId;
        this.fingerprintInput = fingerprintInput;
        this.contentFingerprint = contentFingerprint;
        this.regularMembers = regularMembers;
        this.unsupportedMatchingEntries = unsupportedMatchingEntries;
    }

    public static RepositoryCaptureAttestation verified(
            String sourceId,
            String snapshotId,
            RepositoryCaptureFingerprintInput fingerprintInput,
            RepositoryCaptureFingerprint contentFingerprint,
            List<AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference> regularMembers,
            List<RepositoryCaptureFingerprintInput.UnsupportedMatchingEntry> unsupportedMatchingEntries
    ) {
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(snapshotId, "snapshotId");
        Objects.requireNonNull(fingerprintInput, "fingerprintInput");
        Objects.requireNonNull(contentFingerprint, "contentFingerprint");
        regularMembers = List.copyOf(Objects.requireNonNull(regularMembers, "regularMembers"));
        unsupportedMatchingEntries = List.copyOf(
                Objects.requireNonNull(unsupportedMatchingEntries, "unsupportedMatchingEntries"));
        if (!sourceId.equals(fingerprintInput.sourceId())) {
            throw new IllegalArgumentException("capture identity sourceId must equal fingerprint input sourceId");
        }
        if (!RepositoryCaptureFingerprintEncoder.fingerprint(fingerprintInput).equals(contentFingerprint)) {
            throw new IllegalArgumentException("Repository Capture fingerprint does not match canonical input");
        }
        if (regularMembers.size() != fingerprintInput.capturedMembers().size()) {
            throw new IllegalArgumentException("attested regular members must exactly match capture input");
        }
        for (int index = 0; index < regularMembers.size(); index++) {
            var reference = regularMembers.get(index);
            var captured = fingerprintInput.capturedMembers().get(index);
            if (!sourceId.equals(reference.parentSourceId())
                    || !snapshotId.equals(reference.parentSnapshotId())
                    || !contentFingerprint.equals(reference.parentContentFingerprint())
                    || !captured.normalizedRepositoryRelativePath()
                    .equals(reference.normalizedRepositoryRelativePath())
                    || !captured.rawByteLength().equals(reference.rawByteLength())
                    || !captured.rawMemberFingerprint().equals(reference.rawMemberFingerprint())) {
                throw new IllegalArgumentException(
                        "attested regular members must exactly preserve capture identity, membership, and order");
            }
        }
        if (!unsupportedMatchingEntries.equals(fingerprintInput.unsupportedMatchingEntries())) {
            throw new IllegalArgumentException(
                    "attested unsupported entries must exactly preserve capture membership and order");
        }
        return new RepositoryCaptureAttestation(
                sourceId, snapshotId, fingerprintInput, contentFingerprint, regularMembers,
                unsupportedMatchingEntries);
    }

    public String sourceId() { return sourceId; }
    public String snapshotId() { return snapshotId; }
    public RepositoryCaptureFingerprintInput fingerprintInput() { return fingerprintInput; }
    public RepositoryCaptureFingerprint contentFingerprint() { return contentFingerprint; }
    public List<AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference> regularMembers() {
        return regularMembers;
    }
    public List<RepositoryCaptureFingerprintInput.UnsupportedMatchingEntry> unsupportedMatchingEntries() {
        return unsupportedMatchingEntries;
    }
}
