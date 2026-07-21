package ru.kuznetsov.qagraph.change.materialization;

/**
 * Explicit all-or-nothing Proposed Model materialization outcome.
 */
public sealed interface ProposedModelMaterializationResult
        permits ProposedModelMaterialized,
        ProposedModelMaterializationFailure {
}
