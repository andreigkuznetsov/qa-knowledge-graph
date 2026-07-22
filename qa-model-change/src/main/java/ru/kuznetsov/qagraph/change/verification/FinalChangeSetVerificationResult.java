package ru.kuznetsov.qagraph.change.verification;

public sealed interface FinalChangeSetVerificationResult permits
        VerifiedChangeSet, RejectedChangeSet {
}
