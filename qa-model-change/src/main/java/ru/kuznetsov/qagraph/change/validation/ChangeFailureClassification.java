package ru.kuznetsov.qagraph.change.validation;

/**
 * Primary Canonical Change failure classification and explicit precedence.
 */
public enum ChangeFailureClassification {
    UNSUPPORTED(0),
    STRUCTURALLY_INVALID(1),
    AMBIGUOUS(2),
    UNVERIFIABLE(3);

    private final int precedence;

    ChangeFailureClassification(int precedence) {
        this.precedence = precedence;
    }

    public int precedence() {
        return precedence;
    }
}
