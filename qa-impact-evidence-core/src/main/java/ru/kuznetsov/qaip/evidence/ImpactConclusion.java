package ru.kuznetsov.qaip.evidence;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Authoritative contextual impact conclusion with proof or explicit knowledge limits. */
public record ImpactConclusion(SubjectArtifactRef subject, ImpactClassification classification,
        Optional<ImpactProof> proof, List<UnknownReason> unknownReasons,
        EvidenceSnapshotRef snapshot, SliceAnalysisContext context,
        List<RejectedEvidenceReference> rejectedEvidence) {
    public ImpactConclusion {
        Objects.requireNonNull(subject); Objects.requireNonNull(classification); Objects.requireNonNull(proof);
        Objects.requireNonNull(snapshot); Objects.requireNonNull(context);
        unknownReasons = List.copyOf(Objects.requireNonNull(unknownReasons)).stream()
                .distinct().sorted(Comparator.comparingInt(Enum::ordinal)).toList();
        rejectedEvidence = List.copyOf(Objects.requireNonNull(rejectedEvidence)).stream()
                .sorted(Comparator.comparing(RejectedEvidenceReference::datumId)).toList();
        if (classification == ImpactClassification.AFFECTED && (proof.isEmpty() || !unknownReasons.isEmpty()))
            throw new IllegalArgumentException("AFFECTED requires proof and no unknown reasons");
        if (classification == ImpactClassification.UNKNOWN && (proof.isPresent() || unknownReasons.isEmpty()))
            throw new IllegalArgumentException("UNKNOWN requires reasons and no proof");
    }
}
