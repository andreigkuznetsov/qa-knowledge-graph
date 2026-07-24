package ru.kuznetsov.qaip.evidence;

/** Source-local artifact selected for analysis. */
public record SubjectArtifactRef(String localArtifactId) {
    public SubjectArtifactRef { EvidenceSnapshotRef.requireText(localArtifactId, "localArtifactId"); }
}
