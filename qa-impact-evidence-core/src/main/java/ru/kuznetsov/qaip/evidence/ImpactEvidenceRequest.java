package ru.kuznetsov.qaip.evidence;

import ru.kuznetsov.qagraph.change.verification.VerifiedChangeSet;
import java.util.Objects;

/** Complete explicit invocation boundary for impact-evidence analysis. */
public record ImpactEvidenceRequest(VerifiedChangeSet verifiedChangeSet,
        FrozenEvidenceManifest manifest, SubjectArtifactRef subject,
        SliceAnalysisContext context) {
    public ImpactEvidenceRequest {
        Objects.requireNonNull(verifiedChangeSet, "verifiedChangeSet must not be null");
        Objects.requireNonNull(manifest, "manifest must not be null");
        Objects.requireNonNull(subject, "subject must not be null");
        Objects.requireNonNull(context, "context must not be null");
    }
}
