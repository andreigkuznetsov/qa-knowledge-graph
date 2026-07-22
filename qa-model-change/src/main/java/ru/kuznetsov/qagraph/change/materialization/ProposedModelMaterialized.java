package ru.kuznetsov.qagraph.change.materialization;

import ru.kuznetsov.qagraph.change.base.BaseChangeSetResult;
import ru.kuznetsov.qagraph.change.root.CanonicalBaseModelEvidence;
import java.util.Objects;

/** Successful materialization bound to mandatory source evidence. */
public final class ProposedModelMaterialized implements ProposedModelMaterializationResult {
    private final ProposedArtifactModel proposedModel;
    private final CanonicalBaseModelEvidence baseEvidence;
    private final BaseChangeSetResult sourceResult;

    ProposedModelMaterialized(ProposedArtifactModel model,
                              CanonicalBaseModelEvidence baseEvidence,
                              BaseChangeSetResult sourceResult) {
        this.proposedModel = Objects.requireNonNull(model);
        this.baseEvidence = Objects.requireNonNull(baseEvidence);
        this.sourceResult = Objects.requireNonNull(sourceResult);
        if (sourceResult.baseEvidence() != baseEvidence)
            throw new IllegalArgumentException("sourceResult must own baseEvidence");
    }
    public ProposedArtifactModel proposedModel() { return proposedModel; }
    public CanonicalBaseModelEvidence baseEvidence() { return baseEvidence; }
    public BaseChangeSetResult sourceResult() { return sourceResult; }
    @Override public boolean equals(Object o) { return o instanceof ProposedModelMaterialized that && proposedModel.equals(that.proposedModel) && baseEvidence == that.baseEvidence && sourceResult.equals(that.sourceResult); }
    @Override public int hashCode() { return Objects.hash(proposedModel, System.identityHashCode(baseEvidence), sourceResult); }
}
