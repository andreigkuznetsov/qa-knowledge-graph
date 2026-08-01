package ru.kuznetsov.qaip.core.application.query.trace;

import java.util.List;
import java.util.Objects;

public record TraceResult(
        String startNodeId,
        List<TraceNode> nodes,
        List<TraceRelationship> relationships
) {
    public TraceResult {
        Objects.requireNonNull(startNodeId, "startNodeId");
        if (startNodeId.isBlank()) throw new IllegalArgumentException("startNodeId must not be blank");
        nodes = List.copyOf(nodes);
        relationships = List.copyOf(relationships);
    }
}
