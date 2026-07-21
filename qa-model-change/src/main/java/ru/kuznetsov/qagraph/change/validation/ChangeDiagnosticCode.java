package ru.kuznetsov.qagraph.change.validation;

/**
 * Stable Phase 3 diagnostic vocabulary and deterministic family priority.
 */
public enum ChangeDiagnosticCode {
    UNSUPPORTED_SCHEMA_VERSION(0),
    CROSS_VERSION_CHANGE_UNSUPPORTED(1),
    STATE_SEMANTICS_UNSUPPORTED(2),
    ADDED_BEFORE_STATE_PRESENT(10),
    ADDED_AFTER_STATE_MISSING(11),
    REMOVED_BEFORE_STATE_MISSING(20),
    REMOVED_AFTER_STATE_PRESENT(21),
    MODIFIED_BEFORE_STATE_MISSING(30),
    MODIFIED_AFTER_STATE_MISSING(31),
    ARTIFACT_CATEGORY_MISMATCH(40),
    ARTIFACT_IDENTITY_MISMATCH(41),
    MODIFIED_STATE_UNCHANGED(50),
    DUPLICATE_CHANGE_TARGET(60),
    CONTRADICTORY_CHANGE_TARGET(61);

    private final int priority;

    ChangeDiagnosticCode(int priority) {
        this.priority = priority;
    }

    public int priority() {
        return priority;
    }
}
