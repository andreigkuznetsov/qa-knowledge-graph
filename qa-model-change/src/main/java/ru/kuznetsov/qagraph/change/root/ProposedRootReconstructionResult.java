package ru.kuznetsov.qagraph.change.root;

public sealed interface ProposedRootReconstructionResult permits
        ProposedRootReconstructed,
        ProposedRootReconstructionFailure {
}
