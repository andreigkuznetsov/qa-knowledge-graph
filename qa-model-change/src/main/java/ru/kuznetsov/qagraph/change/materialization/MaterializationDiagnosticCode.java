package ru.kuznetsov.qagraph.change.materialization;

/**
 * Stable controlled vocabulary for Phase 5 failures.
 */
public enum MaterializationDiagnosticCode {
    CHANGE_SET_NOT_MATERIALIZABLE(0),
    MATERIALIZATION_BASE_EVIDENCE_MISMATCH(1),
    MATERIALIZATION_BASE_UNSUPPORTED(2),
    MATERIALIZATION_DUPLICATE_TARGET_WRITE(10),
    MATERIALIZATION_ADDED_TARGET_PRESENT(11),
    MATERIALIZATION_REMOVED_TARGET_MISSING(12),
    MATERIALIZATION_MODIFIED_TARGET_MISSING(13),
    MATERIALIZATION_STATE_INCONSISTENT(14);

    private final int priority;

    MaterializationDiagnosticCode(int priority) {
        this.priority = priority;
    }

    public int priority() {
        return priority;
    }
}
