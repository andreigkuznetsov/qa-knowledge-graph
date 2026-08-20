package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RawSourceMemberFingerprint;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprint;

import java.math.BigInteger;
import java.util.Objects;

/** Exact ordered Repository Capture member selected for positive Manifest verification. */
public record AdmittedManifestParentMemberReferenceV1(
        String sourceId, String snapshotId, RepositoryCaptureFingerprint repositoryCaptureFingerprint,
        String normalizedRepositoryRelativePath, int orderedPosition, BigInteger rawByteLength,
        RawSourceMemberFingerprint rawSourceMemberFingerprint) {
    public AdmittedManifestParentMemberReferenceV1 {
        text(sourceId); text(snapshotId); Objects.requireNonNull(repositoryCaptureFingerprint);
        text(normalizedRepositoryRelativePath);
        if (orderedPosition < 0) throw new IllegalArgumentException("orderedPosition must be nonnegative");
        Objects.requireNonNull(rawByteLength); Objects.requireNonNull(rawSourceMemberFingerprint);
        if (rawByteLength.signum() < 0) throw new IllegalArgumentException("rawByteLength must be nonnegative");
    }
    private static void text(String value) {
        if (value == null || value.isEmpty()) throw new IllegalArgumentException("text is required");
    }
}
