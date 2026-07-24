package ru.kuznetsov.qaip.evidence;

import ru.kuznetsov.qagraph.model.RelationshipType;
import java.util.Objects;

/** One normalized positive relationship datum in stored source-to-target direction. */
public record RelationshipEvidence(String datumId, EvidenceSnapshotRef snapshot,
        String sourceLocalId, String targetLocalId, RelationshipType relationshipType,
        String sourceNativeType, String normalizationVersion,
        String contentFingerprint, String provenanceId) {
    public RelationshipEvidence {
        EvidenceSnapshotRef.requireText(datumId, "datumId");
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        EvidenceSnapshotRef.requireText(sourceLocalId, "sourceLocalId");
        EvidenceSnapshotRef.requireText(targetLocalId, "targetLocalId");
        Objects.requireNonNull(relationshipType, "relationshipType must not be null");
        EvidenceSnapshotRef.requireText(sourceNativeType, "sourceNativeType");
        EvidenceSnapshotRef.requireText(normalizationVersion, "normalizationVersion");
        EvidenceSnapshotRef.requireText(contentFingerprint, "contentFingerprint");
        EvidenceSnapshotRef.requireText(provenanceId, "provenanceId");
    }
}
