package ru.kuznetsov.qagraph.api;

public record FindingResponse(
        String code,
        String severity,
        String nodeId,
        String nodeType,
        String message,
        String recommendation
) {
}
