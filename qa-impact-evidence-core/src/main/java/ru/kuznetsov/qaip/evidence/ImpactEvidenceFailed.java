package ru.kuznetsov.qaip.evidence;

import java.util.Objects;

/** Analysis that could not form a conclusion. */
public record ImpactEvidenceFailed(ImpactAnalysisFailure failure) implements ImpactEvidenceResult {
    public ImpactEvidenceFailed { Objects.requireNonNull(failure); }
}
