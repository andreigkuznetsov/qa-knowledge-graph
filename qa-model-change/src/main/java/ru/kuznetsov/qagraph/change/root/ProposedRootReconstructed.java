package ru.kuznetsov.qagraph.change.root;

import ru.kuznetsov.qagraph.change.aggregate.AggregateTransitionValid;

import java.util.Objects;

public record ProposedRootReconstructed(
        ProposedCanonicalRoot proposedRoot,
        CanonicalBaseModelEvidence baseEvidence,
        AggregateTransitionValid aggregateTransition
) implements ProposedRootReconstructionResult {
    public ProposedRootReconstructed {
        Objects.requireNonNull(proposedRoot, "proposedRoot must not be null");
        Objects.requireNonNull(baseEvidence, "baseEvidence must not be null");
        Objects.requireNonNull(
                aggregateTransition,
                "aggregateTransition must not be null"
        );
    }
}
