package ru.kuznetsov.qaip.explorer.application;

public record EventPathProjectionIncomplete(
        String projectId,
        String operationId
) implements EventPathProjectionResult {
    public EventPathProjectionIncomplete {
        projectId = EventPathProjectionResult.requireId(projectId, "projectId");
        operationId = EventPathProjectionResult.requireId(operationId, "operationId");
    }
}
