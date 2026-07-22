package ru.kuznetsov.qagraph.change.root;

import ru.kuznetsov.qagraph.change.base.BaseArtifactIndex;
import ru.kuznetsov.qagraph.change.aggregate.AggregateTransitionValid;

public final class RootTestFixtures {
    private RootTestFixtures() { }
    public static CanonicalBaseModelEvidence evidence(
            CanonicalRootContext context, BaseArtifactIndex index) {
        return new CanonicalBaseModelEvidence(context, index);
    }
    public static ProposedRootReconstructed reconstructed(ProposedCanonicalRoot root,
            CanonicalBaseModelEvidence evidence, AggregateTransitionValid aggregate) {
        return new ProposedRootReconstructed(root, evidence, aggregate);
    }
}
