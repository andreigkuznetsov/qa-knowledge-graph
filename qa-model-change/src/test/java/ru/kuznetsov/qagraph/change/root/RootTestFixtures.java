package ru.kuznetsov.qagraph.change.root;

import ru.kuznetsov.qagraph.change.base.BaseArtifactIndex;

public final class RootTestFixtures {
    private RootTestFixtures() { }
    public static CanonicalBaseModelEvidence evidence(
            CanonicalRootContext context, BaseArtifactIndex index) {
        return new CanonicalBaseModelEvidence(context, index);
    }
}
