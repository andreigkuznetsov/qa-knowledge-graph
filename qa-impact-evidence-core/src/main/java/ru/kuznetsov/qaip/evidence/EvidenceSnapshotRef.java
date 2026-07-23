package ru.kuznetsov.qaip.evidence;

import java.util.Objects;

/** Identity and captured-content fingerprint of one frozen source snapshot. */
public record EvidenceSnapshotRef(String sourceId, String snapshotId, String contentFingerprint) {
    public EvidenceSnapshotRef {
        requireText(sourceId, "sourceId"); requireText(snapshotId, "snapshotId");
        requireText(contentFingerprint, "contentFingerprint");
    }
    static void requireText(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
    }
}
