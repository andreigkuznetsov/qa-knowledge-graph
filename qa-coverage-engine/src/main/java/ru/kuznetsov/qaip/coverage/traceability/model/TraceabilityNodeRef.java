package ru.kuznetsov.qaip.coverage.traceability.model;

public record TraceabilityNodeRef(
        String id,
        String type,
        String name,
        String path
) {
}
