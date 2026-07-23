package ru.kuznetsov.qaip.evidence;

import ru.kuznetsov.qagraph.model.NodeType;
import java.util.Objects;

/** Frozen evidence that maps one source-local artifact to a canonical identity state. */
public record ArtifactIdentityAssertion(String assertionId, EvidenceSnapshotRef snapshot,
        String localArtifactId, NodeType nodeType, IdentityResolution resolution,
        String contentFingerprint, String provenanceId) {
    public ArtifactIdentityAssertion {
        EvidenceSnapshotRef.requireText(assertionId, "assertionId");
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        EvidenceSnapshotRef.requireText(localArtifactId, "localArtifactId");
        Objects.requireNonNull(nodeType, "nodeType must not be null");
        Objects.requireNonNull(resolution, "resolution must not be null");
        EvidenceSnapshotRef.requireText(contentFingerprint, "contentFingerprint");
        EvidenceSnapshotRef.requireText(provenanceId, "provenanceId");
    }
}
