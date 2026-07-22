package ru.kuznetsov.qagraph.change.model;

/**
 * Canonical change kinds in their stable defensive tie-breaker order.
 */
public enum ChangeKind {
    ADDED(0),
    MODIFIED(1),
    REMOVED(2);

    private final int canonicalOrder;

    ChangeKind(int canonicalOrder) {
        this.canonicalOrder = canonicalOrder;
    }

    public int canonicalOrder() {
        return canonicalOrder;
    }
}
