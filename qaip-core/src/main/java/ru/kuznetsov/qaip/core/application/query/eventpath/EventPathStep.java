package ru.kuznetsov.qaip.core.application.query.eventpath;

import java.util.Objects;

public record EventPathStep(
        String nodeId,
        EventPathImplementationRole implementationRole,
        String name,
        EventPathImplementationType implementationType,
        String technology
) {
    public EventPathStep {
        nodeId = EventPathProjectNotFound.requireId(nodeId, "nodeId");
        Objects.requireNonNull(implementationRole, "implementationRole");
        name = EventPathProjectNotFound.requireId(name, "name");
        Objects.requireNonNull(implementationType, "implementationType");
        if (technology != null && technology.isBlank()) {
            throw new IllegalArgumentException("technology must be null or non-blank");
        }
    }
}
