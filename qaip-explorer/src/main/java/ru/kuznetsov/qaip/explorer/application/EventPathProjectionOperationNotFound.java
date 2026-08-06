package ru.kuznetsov.qaip.explorer.application;

public record EventPathProjectionOperationNotFound(
        String projectId,
        String operationId
) implements EventPathProjectionResult {
    public EventPathProjectionOperationNotFound {
        projectId = EventPathProjectionResult.requireId(projectId, "projectId");
        operationId = EventPathProjectionResult.requireId(operationId, "operationId");
    }
}
