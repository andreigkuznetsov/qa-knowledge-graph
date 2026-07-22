package ru.kuznetsov.qagraph.change.aggregate;

import ru.kuznetsov.qagraph.change.materialization.ProposedModelMaterialized;

/**
 * Result of Phase 6 cross-artifact validation only.
 */
public sealed interface AggregateTransitionValidationResult
        permits AggregateTransitionValid, AggregateTransitionInvalid {

    ProposedModelMaterialized materialization();
}
