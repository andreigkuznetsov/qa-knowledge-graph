package ru.kuznetsov.qagraph.trace;

public record TraceRelationship(
        String type,
        String from,
        String to
) {
}
