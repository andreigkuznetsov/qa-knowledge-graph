package ru.kuznetsov.qaip.evidence;

import ru.kuznetsov.qagraph.change.model.CanonicalIdentity;
import java.util.List;
import java.util.Objects;

/** Ordered, contiguous analyzer-owned qualified influence path. */
public final class RelationshipPathProof implements ImpactProof {
    private final CanonicalIdentity changedIdentity;
    private final CanonicalIdentity subjectIdentity;
    private final List<QualifiedPathStep> steps;
    RelationshipPathProof(CanonicalIdentity changedIdentity, CanonicalIdentity subjectIdentity,
                          List<QualifiedPathStep> steps) {
        this.changedIdentity=Objects.requireNonNull(changedIdentity);
        this.subjectIdentity=Objects.requireNonNull(subjectIdentity);
        this.steps=List.copyOf(Objects.requireNonNull(steps));
        if (steps.isEmpty()) throw new IllegalArgumentException("path proof must contain a step");
        CanonicalIdentity expected=changedIdentity;
        for (QualifiedPathStep step:steps) { if(!step.propagationFrom().equals(expected)) throw new IllegalArgumentException("path is not contiguous"); expected=step.propagationTo(); }
        if(!expected.equals(subjectIdentity)) throw new IllegalArgumentException("path does not end at subject");
    }
    public CanonicalIdentity changedIdentity(){return changedIdentity;}
    public CanonicalIdentity subjectIdentity(){return subjectIdentity;}
    public List<QualifiedPathStep> steps(){return steps;}
    @Override public boolean equals(Object o){return o instanceof RelationshipPathProof p && changedIdentity.equals(p.changedIdentity)&&subjectIdentity.equals(p.subjectIdentity)&&steps.equals(p.steps);}
    @Override public int hashCode(){return Objects.hash(changedIdentity,subjectIdentity,steps);}
}
