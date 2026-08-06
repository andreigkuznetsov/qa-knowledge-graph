package ru.kuznetsov.qaip.explorer.application;

public record EventPathProjectionNotEventDriven(
        String projectId,
        String operationId
) implements EventPathProjectionResult {
    public EventPathProjectionNotEventDriven {
        projectId = EventPathProjectionResult.requireId(projectId, "projectId");
        operationId = EventPathProjectionResult.requireId(operationId, "operationId");
    }
}
