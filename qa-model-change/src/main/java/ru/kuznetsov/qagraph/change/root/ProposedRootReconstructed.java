package ru.kuznetsov.qagraph.change.root;
import ru.kuznetsov.qagraph.change.aggregate.AggregateTransitionValid;
import java.util.Objects;
/** Reconstruction success created only by {@link ProposedCanonicalRootReconstructor}. */
public final class ProposedRootReconstructed implements ProposedRootReconstructionResult {
    private final ProposedCanonicalRoot proposedRoot;
    private final CanonicalBaseModelEvidence baseEvidence;
    private final AggregateTransitionValid aggregateTransition;
    ProposedRootReconstructed(ProposedCanonicalRoot root, CanonicalBaseModelEvidence base, AggregateTransitionValid aggregate) {
        proposedRoot = Objects.requireNonNull(root); baseEvidence = Objects.requireNonNull(base); aggregateTransition = Objects.requireNonNull(aggregate);
    }
    public ProposedCanonicalRoot proposedRoot() { return proposedRoot; }
    public CanonicalBaseModelEvidence baseEvidence() { return baseEvidence; }
    public AggregateTransitionValid aggregateTransition() { return aggregateTransition; }
    @Override public boolean equals(Object o) { return o instanceof ProposedRootReconstructed that && proposedRoot == that.proposedRoot && baseEvidence == that.baseEvidence && aggregateTransition.equals(that.aggregateTransition); }
    @Override public int hashCode() { return Objects.hash(System.identityHashCode(proposedRoot), System.identityHashCode(baseEvidence), aggregateTransition); }
}
