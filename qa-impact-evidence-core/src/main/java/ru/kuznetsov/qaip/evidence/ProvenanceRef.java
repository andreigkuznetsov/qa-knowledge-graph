package ru.kuznetsov.qaip.evidence;

/** Minimal lineage reference for a normalized datum. */
public record ProvenanceRef(String provenanceId, String originLocator, String originHash,
                            String normalizationActivity, String normalizationVersion) {
    public ProvenanceRef {
        EvidenceSnapshotRef.requireText(provenanceId, "provenanceId");
        EvidenceSnapshotRef.requireText(originLocator, "originLocator");
        EvidenceSnapshotRef.requireText(originHash, "originHash");
        EvidenceSnapshotRef.requireText(normalizationActivity, "normalizationActivity");
        EvidenceSnapshotRef.requireText(normalizationVersion, "normalizationVersion");
    }
}
