package ru.kuznetsov.qaip.evidence;

import ru.kuznetsov.qagraph.change.model.*;
import ru.kuznetsov.qagraph.change.verification.VerifiedChangeSet;
import ru.kuznetsov.qagraph.model.NodeType;

import java.util.Objects;

/** Exact analyzer-owned accepted declaration proving direct impact. */
public final class DirectChangeProof implements ImpactProof {
    private final VerifiedChangeSet verifiedChangeSet;
    private final int declarationIndex;
    private final CanonicalIdentity identity;
    private final ArtifactCategory category;
    private final ChangeKind changeKind;

    DirectChangeProof(VerifiedChangeSet verifiedChangeSet, int declarationIndex,
                      ArtifactIdentityAssertion subjectAssertion) {
        this.verifiedChangeSet = Objects.requireNonNull(verifiedChangeSet);
        Objects.requireNonNull(subjectAssertion);
        if (!(subjectAssertion.resolution() instanceof ResolvedIdentity resolved))
            throw new IllegalArgumentException("direct proof subject must be resolved");
        if (declarationIndex < 0 || declarationIndex >= verifiedChangeSet.declaredChangeSet().changes().size())
            throw new IllegalArgumentException("declarationIndex is outside the accepted change set");
        DeclaredChange actual = verifiedChangeSet.declaredChangeSet().changes().get(declarationIndex);
        if (!actual.identity().equals(resolved.identity()) || actual.category() != ArtifactCategory.NODE
                || subjectAssertion.nodeType() != NodeType.BUSINESS_RULE || !supportedType(actual))
            throw new IllegalArgumentException("declaration does not match the BUSINESS_RULE subject");
        this.declarationIndex = declarationIndex;
        this.identity = actual.identity();
        this.category = actual.category();
        this.changeKind = actual.kind();
    }
    public VerifiedChangeSet verifiedChangeSet() { return verifiedChangeSet; }
    public int declarationIndex() { return declarationIndex; }
    public CanonicalIdentity identity() { return identity; }
    public ArtifactCategory category() { return category; }
    public NodeType nodeType() { return NodeType.BUSINESS_RULE; }
    public ChangeKind changeKind() { return changeKind; }
    private boolean supportedType(DeclaredChange change) {
        NodeType before = type(change.beforeState());
        NodeType after = type(change.afterState());
        return switch (change.kind()) {
            case ADDED -> after == NodeType.BUSINESS_RULE;
            case MODIFIED -> before == NodeType.BUSINESS_RULE && after == NodeType.BUSINESS_RULE;
            case REMOVED -> before == NodeType.BUSINESS_RULE;
        };
    }
    private NodeType type(java.util.Optional<ArtifactState> state) {
        if (state.isEmpty() || !(state.get() instanceof NodeArtifactState node)) return null;
        return NodeType.from(node.snapshot().path("type").asText(null));
    }
    @Override public boolean equals(Object o) { return o instanceof DirectChangeProof p
            && verifiedChangeSet.equals(p.verifiedChangeSet) && declarationIndex == p.declarationIndex
            && identity.equals(p.identity) && category == p.category && changeKind == p.changeKind; }
    @Override public int hashCode() { return Objects.hash(verifiedChangeSet, declarationIndex, identity, category, changeKind); }
}
