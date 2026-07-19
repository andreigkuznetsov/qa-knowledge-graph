package ru.kuznetsov.qagraph.trace;

import java.util.List;

public record TracePath(
        boolean found,
        List<TraceNode> nodes,
        List<TraceRelationship> relationships
) {
    public TracePath {
        nodes = List.copyOf(nodes);
        relationships = List.copyOf(relationships);
    }

    public static TracePath notFound() {
        return new TracePath(false, List.of(), List.of());
    }
}
