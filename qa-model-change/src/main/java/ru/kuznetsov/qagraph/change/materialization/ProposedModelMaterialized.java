package ru.kuznetsov.qagraph.change.materialization;

import java.util.Objects;

/**
 * Deterministically materialized candidate artifact model.
 */
public record ProposedModelMaterialized(ProposedArtifactModel proposedModel)
        implements ProposedModelMaterializationResult {

    public ProposedModelMaterialized {
        Objects.requireNonNull(
                proposedModel,
                "proposedModel must not be null"
        );
    }
}
