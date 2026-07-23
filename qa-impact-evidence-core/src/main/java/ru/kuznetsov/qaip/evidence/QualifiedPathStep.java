package ru.kuznetsov.qaip.evidence;

import ru.kuznetsov.qagraph.change.model.CanonicalIdentity;
import java.util.Objects;

/** One analyzer-qualified dependency-to-dependent propagation step. */
public final class QualifiedPathStep {
    private final CanonicalIdentity propagationFrom;
    private final CanonicalIdentity propagationTo;
    private final RelationshipEvidence evidence;
    QualifiedPathStep(QualifiedRelationship qualified) {
        Objects.requireNonNull(qualified);
        this.propagationFrom = qualified.propagationFrom();
        this.propagationTo = qualified.propagationTo();
        this.evidence = qualified.evidence();
    }
    public CanonicalIdentity propagationFrom() { return propagationFrom; }
    public CanonicalIdentity propagationTo() { return propagationTo; }
    public RelationshipEvidence evidence() { return evidence; }
    @Override public boolean equals(Object o) { return o instanceof QualifiedPathStep s
            && propagationFrom.equals(s.propagationFrom) && propagationTo.equals(s.propagationTo)
            && evidence.equals(s.evidence); }
    @Override public int hashCode() { return Objects.hash(propagationFrom, propagationTo, evidence); }
}
