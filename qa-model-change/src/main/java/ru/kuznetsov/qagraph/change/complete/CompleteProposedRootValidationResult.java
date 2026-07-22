package ru.kuznetsov.qagraph.change.complete;

public sealed interface CompleteProposedRootValidationResult permits
        CompleteProposedRootValid,
        CompleteProposedRootInvalid {
}
