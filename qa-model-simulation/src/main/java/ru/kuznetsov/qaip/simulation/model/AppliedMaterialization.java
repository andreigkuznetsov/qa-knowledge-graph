package ru.kuznetsov.qaip.simulation.model;

import java.util.Objects;

public record AppliedMaterialization(
        String taskId,
        String createdNodeId,
        String createdRelationshipId
) {
    public AppliedMaterialization {
        Objects.requireNonNull(taskId, "taskId must not be null");
        Objects.requireNonNull(createdNodeId,
                "createdNodeId must not be null");
        Objects.requireNonNull(createdRelationshipId,
                "createdRelationshipId must not be null");
    }
}
