package ru.kuznetsov.qagraph.api;

import java.util.List;

public record TraceResponse(
        String modelId,
        String fromNodeId,
        String toNodeId,
        boolean found,
        int relationshipCount,
        List<TracePathElement> path
) {
    public TraceResponse {
        path = List.copyOf(path);
    }
}
