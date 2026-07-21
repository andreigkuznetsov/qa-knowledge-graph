package ru.kuznetsov.qagraph.change.materialization;

/**
 * Stable category of Proposed Model materialization failure.
 */
public enum MaterializationFailureKind {
    INELIGIBLE_CHANGE_SET(0),
    STALE_BASE_EVIDENCE(1),
    UNSUPPORTED_BASE(2),
    CONSISTENCY_FAILURE(3);

    private final int priority;

    MaterializationFailureKind(int priority) {
        this.priority = priority;
    }

    public int priority() {
        return priority;
    }
}
