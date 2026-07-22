package ru.kuznetsov.qagraph.change.aggregate;

/**
 * Stable endpoint ordering using Canonical QA Model v0.1 terminology.
 */
public enum RelationshipEndpointRole {
    SOURCE(0, "from"),
    TARGET(1, "to"),
    MODEL(2, "model");

    private final int rank;
    private final String property;

    RelationshipEndpointRole(int rank, String property) {
        this.rank = rank;
        this.property = property;
    }

    public int rank() {
        return rank;
    }

    public String property() {
        return property;
    }
}
