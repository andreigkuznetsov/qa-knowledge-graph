package ru.kuznetsov.qaip.core.application.query.trace;

import java.util.Objects;

public record TraceNode(String nodeId, String nodeType) {
    public TraceNode {
        nodeId = requireNonBlank(nodeId, "nodeId");
        Objects.requireNonNull(nodeType, "nodeType");
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
