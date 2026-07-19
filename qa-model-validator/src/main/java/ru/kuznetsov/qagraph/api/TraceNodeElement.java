package ru.kuznetsov.qagraph.api;

public record TraceNodeElement(
        String nodeId,
        String nodeType,
        String nodeName
) implements TracePathElement {
}
