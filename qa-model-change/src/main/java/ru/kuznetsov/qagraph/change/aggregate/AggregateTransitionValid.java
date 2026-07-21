package ru.kuznetsov.qagraph.change.aggregate;

import ru.kuznetsov.qagraph.change.materialization.ProposedModelMaterialized;

import java.util.Objects;

/**
 * Materialized model that passed only the implemented aggregate invariants.
 */
public record AggregateTransitionValid(
        ProposedModelMaterialized materialization
) implements AggregateTransitionValidationResult {

    public AggregateTransitionValid {
        Objects.requireNonNull(
                materialization,
                "materialization must not be null"
        );
    }
}
