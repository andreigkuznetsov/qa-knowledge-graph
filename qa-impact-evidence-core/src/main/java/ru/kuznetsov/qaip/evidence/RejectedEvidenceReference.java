package ru.kuznetsov.qaip.evidence;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Stable reference and reasons for relationship evidence excluded from proof. */
public record RejectedEvidenceReference(String datumId, List<QualificationReason> reasons) {
    public RejectedEvidenceReference {
        EvidenceSnapshotRef.requireText(datumId, "datumId");
        reasons = List.copyOf(Objects.requireNonNull(reasons)).stream()
                .distinct().sorted(Comparator.comparingInt(Enum::ordinal)).toList();
        if (reasons.isEmpty()) throw new IllegalArgumentException("reasons must not be empty");
    }
}
