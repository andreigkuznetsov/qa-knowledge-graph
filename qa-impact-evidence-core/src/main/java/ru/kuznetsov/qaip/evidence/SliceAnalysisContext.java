package ru.kuznetsov.qaip.evidence;

/** Explicit versions that define the analysis semantics. */
public record SliceAnalysisContext(String qualificationVersion, String influenceVersion,
                                   String algorithmVersion) {
    public SliceAnalysisContext {
        EvidenceSnapshotRef.requireText(qualificationVersion, "qualificationVersion");
        EvidenceSnapshotRef.requireText(influenceVersion, "influenceVersion");
        EvidenceSnapshotRef.requireText(algorithmVersion, "algorithmVersion");
    }
    public static SliceAnalysisContext supported() {
        return new SliceAnalysisContext(ImpactEvidenceVersions.QUALIFICATION,
                ImpactEvidenceVersions.INFLUENCE, ImpactEvidenceVersions.ALGORITHM);
    }
}
