package ru.kuznetsov.qaip.core.application.query.nodedetails;

import java.util.Objects;

public record NodeDetailsResult(
        String nodeId,
        String nodeType,
        String name,
        String description,
        String status) {

    public NodeDetailsResult {
        nodeId = requireNonBlank(nodeId, "nodeId");
        nodeType = requireNonBlank(nodeType, "nodeType");
        name = requireNonBlank(name, "name");
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
