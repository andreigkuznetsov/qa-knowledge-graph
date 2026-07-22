package ru.kuznetsov.qagraph.change.aggregate;

/**
 * Stable controlled Phase 6 diagnostic vocabulary.
 */
public enum AggregateTransitionDiagnosticCode {
    PROPOSED_MODEL_VERSION_UNSUPPORTED(0),
    PROPOSED_ARTIFACT_VERSION_MISMATCH(1),
    RELATIONSHIP_ENDPOINT_MODEL_VERSION_MISMATCH(2),
    RELATIONSHIP_SOURCE_ENDPOINT_MISSING(10),
    RELATIONSHIP_TARGET_ENDPOINT_MISSING(11),
    RELATIONSHIP_SOURCE_ENDPOINT_INVALID(12),
    RELATIONSHIP_TARGET_ENDPOINT_INVALID(13),
    RELATIONSHIP_SOURCE_NODE_NOT_FOUND(20),
    RELATIONSHIP_TARGET_NODE_NOT_FOUND(21);

    private final int priority;

    AggregateTransitionDiagnosticCode(int priority) {
        this.priority = priority;
    }

    public int priority() {
        return priority;
    }
}
