package ru.kuznetsov.qaip.evidence;

/** Stable machine-readable diagnostic for a failed analysis. */
public record AnalysisDiagnostic(String code, String message, String objectId) {
    public AnalysisDiagnostic {
        EvidenceSnapshotRef.requireText(code, "code"); EvidenceSnapshotRef.requireText(message, "message");
        objectId = objectId == null ? "" : objectId;
    }
}
