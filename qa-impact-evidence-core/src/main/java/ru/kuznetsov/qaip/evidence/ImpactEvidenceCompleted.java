package ru.kuznetsov.qaip.evidence;

import java.util.Objects;

/** Successful analysis carrying one valid conclusion. */
public record ImpactEvidenceCompleted(ImpactConclusion conclusion) implements ImpactEvidenceResult {
    public ImpactEvidenceCompleted { Objects.requireNonNull(conclusion); }
}
