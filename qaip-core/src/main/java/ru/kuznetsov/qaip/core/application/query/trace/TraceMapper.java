package ru.kuznetsov.qaip.core.application.query.trace;

import java.util.Objects;

public final class TraceMapper {
    public TraceResult map(TraceGraph traceGraph) {
        Objects.requireNonNull(traceGraph, "traceGraph");
        return new TraceResult(
                traceGraph.startNodeId(),
                traceGraph.nodes().stream()
                        .map(node -> new TraceNode(node.id(), node.type()))
                        .toList(),
                traceGraph.relationships().stream()
                        .map(relationship -> new TraceRelationship(
                                relationship.id(),
                                relationship.from(),
                                relationship.to(),
                                relationship.type()))
                        .toList());
    }
}
