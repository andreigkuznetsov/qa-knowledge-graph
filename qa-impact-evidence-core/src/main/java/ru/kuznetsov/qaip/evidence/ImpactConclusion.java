package ru.kuznetsov.qaip.evidence;

import java.util.*;

/** Analyzer-owned contextual conclusion anchored by the exact subject assertion. */
public final class ImpactConclusion {
    private final ArtifactIdentityAssertion subjectAssertion;
    private final ImpactClassification classification;
    private final Optional<ImpactProof> proof;
    private final List<UnknownReason> unknownReasons;
    private final SliceAnalysisContext context;
    private final List<RejectedEvidenceReference> rejectedEvidence;

    ImpactConclusion(ArtifactIdentityAssertion subjectAssertion,
            ImpactClassification classification, Optional<ImpactProof> proof,
            List<UnknownReason> unknownReasons, SliceAnalysisContext context,
            List<RejectedEvidenceReference> rejectedEvidence) {
        this.subjectAssertion=Objects.requireNonNull(subjectAssertion);
        this.classification=Objects.requireNonNull(classification);
        this.proof=Objects.requireNonNull(proof);
        this.context=Objects.requireNonNull(context);
        this.unknownReasons=List.copyOf(Objects.requireNonNull(unknownReasons)).stream()
                .distinct().sorted(Comparator.comparingInt(Enum::ordinal)).toList();
        this.rejectedEvidence=List.copyOf(Objects.requireNonNull(rejectedEvidence)).stream()
                .sorted(Comparator.comparing(RejectedEvidenceReference::datumId)).toList();
        if (classification == ImpactClassification.AFFECTED && (proof.isEmpty() || !this.unknownReasons.isEmpty()))
            throw new IllegalArgumentException("AFFECTED requires proof and no unknown reasons");
        if (classification == ImpactClassification.UNKNOWN && (proof.isPresent() || this.unknownReasons.isEmpty()))
            throw new IllegalArgumentException("UNKNOWN requires reasons and no proof");
        if (proof.isPresent()) validateBinding(proof.get());
        if (subjectAssertion.resolution() instanceof UnresolvedIdentity
                && classification != ImpactClassification.UNKNOWN)
            throw new IllegalArgumentException("unresolved subject can only produce UNKNOWN");
    }

    private void validateBinding(ImpactProof value) {
        if (!(subjectAssertion.resolution() instanceof ResolvedIdentity resolved))
            throw new IllegalArgumentException("proof requires resolved subject");
        if (value instanceof DirectChangeProof direct && !direct.identity().equals(resolved.identity()))
            throw new IllegalArgumentException("direct proof identity differs from subject");
        if (value instanceof RelationshipPathProof path
                && (!path.subjectIdentity().equals(resolved.identity())
                || !path.snapshot().equals(subjectAssertion.snapshot())))
            throw new IllegalArgumentException("path proof context differs from subject assertion");
    }

    public ArtifactIdentityAssertion subjectAssertion() { return subjectAssertion; }
    public SubjectArtifactRef subject() { return new SubjectArtifactRef(subjectAssertion.localArtifactId()); }
    public ImpactClassification classification() { return classification; }
    public Optional<ImpactProof> proof() { return proof; }
    public List<UnknownReason> unknownReasons() { return unknownReasons; }
    public EvidenceSnapshotRef snapshot() { return subjectAssertion.snapshot(); }
    public SliceAnalysisContext context() { return context; }
    public List<RejectedEvidenceReference> rejectedEvidence() { return rejectedEvidence; }
    @Override public boolean equals(Object o) { return o instanceof ImpactConclusion c
            && subjectAssertion.equals(c.subjectAssertion) && classification == c.classification
            && proof.equals(c.proof) && unknownReasons.equals(c.unknownReasons)
            && context.equals(c.context) && rejectedEvidence.equals(c.rejectedEvidence); }
    @Override public int hashCode() { return Objects.hash(subjectAssertion, classification, proof,
            unknownReasons, context, rejectedEvidence); }
}
