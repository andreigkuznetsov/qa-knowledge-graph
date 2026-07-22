package ru.kuznetsov.qagraph.change.aggregate;

/**
 * Aggregate-specific failure meaning and explicit precedence.
 */
public enum AggregateTransitionFailureKind {
    UNSUPPORTED(0),
    STRUCTURALLY_INVALID(1),
    DANGLING_REFERENCE(2);

    private final int precedence;

    AggregateTransitionFailureKind(int precedence) {
        this.precedence = precedence;
    }

    public int precedence() {
        return precedence;
    }
}
