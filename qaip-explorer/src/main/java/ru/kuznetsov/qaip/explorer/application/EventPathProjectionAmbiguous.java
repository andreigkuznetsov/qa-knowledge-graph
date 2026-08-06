package ru.kuznetsov.qaip.explorer.application;

public record EventPathProjectionAmbiguous(
        String projectId,
        String operationId
) implements EventPathProjectionResult {
    public EventPathProjectionAmbiguous {
        projectId = EventPathProjectionResult.requireId(projectId, "projectId");
        operationId = EventPathProjectionResult.requireId(operationId, "operationId");
    }
}
