package ru.kuznetsov.qaip.core.application.query.nodedetails;

import java.util.Objects;

public record NodeDetailsFound(NodeDetailsResult details) implements NodeDetailsQueryResult {
    public NodeDetailsFound {
        Objects.requireNonNull(details, "details");
    }
}
