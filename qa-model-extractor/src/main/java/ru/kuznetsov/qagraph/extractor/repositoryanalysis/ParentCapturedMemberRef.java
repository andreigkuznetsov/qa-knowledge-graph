package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RawSourceMemberFingerprint;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprint;

import java.util.Objects;

/** Exact ADR-014 anti-substitution reference to one parent Repository Capture member. */
public record ParentCapturedMemberRef(
        String parentSourceId,
        String parentSnapshotId,
        RepositoryCaptureFingerprint parentContentFingerprint,
        String normalizedRepositoryRelativePath,
        long rawByteLength,
        RawSourceMemberFingerprint rawMemberFingerprint
) {
    public ParentCapturedMemberRef {
        parentSourceId = requireNonBlank(parentSourceId, "parentSourceId");
        parentSnapshotId = requireNonBlank(parentSnapshotId, "parentSnapshotId");
        Objects.requireNonNull(parentContentFingerprint, "parentContentFingerprint");
        normalizedRepositoryRelativePath = requireNonBlank(
                normalizedRepositoryRelativePath, "normalizedRepositoryRelativePath");
        if (rawByteLength < 0) throw new IllegalArgumentException("rawByteLength must not be negative");
        Objects.requireNonNull(rawMemberFingerprint, "rawMemberFingerprint");
    }

    static ParentCapturedMemberRef from(
            ScenarioRepositoryCaptureSnapshotCandidate parent,
            ScenarioManifestStableCaptureResult.CapturedMember member
    ) {
        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(member, "member");
        ParentCapturedMemberRef reference = new ParentCapturedMemberRef(
                parent.sourceId(),
                parent.snapshotId(),
                parent.contentFingerprint(),
                member.repositoryRelativePath(),
                member.rawByteLength(),
                member.rawMemberFingerprint());
        reference.verifyAgainst(parent);
        return reference;
    }

    /** Fails when this reference does not resolve to exactly one matching member in the parent candidate. */
    public void verifyAgainst(ScenarioRepositoryCaptureSnapshotCandidate parent) {
        Objects.requireNonNull(parent, "parent");
        if (!parentSourceId.equals(parent.sourceId())
                || !parentSnapshotId.equals(parent.snapshotId())
                || !parentContentFingerprint.equals(parent.contentFingerprint())) {
            throw new IllegalArgumentException("parent Repository Capture identity does not match");
        }

        var matches = parent.members().stream()
                .filter(member -> normalizedRepositoryRelativePath.equals(member.repositoryRelativePath()))
                .toList();
        if (matches.size() != 1) {
            throw new IllegalArgumentException("parent member reference must resolve exactly once");
        }
        ScenarioManifestStableCaptureResult.CapturedMember member = matches.getFirst();
        if (rawByteLength != member.rawByteLength()
                || !rawMemberFingerprint.equals(member.rawMemberFingerprint())) {
            throw new IllegalArgumentException("parent member length or raw fingerprint does not match");
        }
    }

    private static String requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }
}
