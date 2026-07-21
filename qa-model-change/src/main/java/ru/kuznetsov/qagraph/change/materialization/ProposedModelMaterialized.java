package ru.kuznetsov.qagraph.change.materialization;

import ru.kuznetsov.qagraph.change.root.CanonicalBaseModelEvidence;

import java.util.Objects;
import java.util.Optional;

/**
 * Deterministically materialized candidate artifact model.
 */
public record ProposedModelMaterialized(
        ProposedArtifactModel proposedModel,
        Optional<CanonicalBaseModelEvidence> baseEvidence
)
        implements ProposedModelMaterializationResult {

    public ProposedModelMaterialized {
        Objects.requireNonNull(
                proposedModel,
                "proposedModel must not be null"
        );
        Objects.requireNonNull(baseEvidence, "baseEvidence must not be null");
    }

    public ProposedModelMaterialized(ProposedArtifactModel proposedModel) {
        this(proposedModel, Optional.empty());
    }
}
