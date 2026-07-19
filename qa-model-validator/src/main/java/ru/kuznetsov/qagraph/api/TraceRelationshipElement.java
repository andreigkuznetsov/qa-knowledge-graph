package ru.kuznetsov.qagraph.api;

public record TraceRelationshipElement(
        String relationshipType,
        String fromNodeId,
        String toNodeId
) implements TracePathElement {
}
