package ru.kuznetsov.qaip.core.application.query.eventpath;

public record EventPathNotEventDriven(String projectId, String operationId) implements EventPathQueryResult {
    public EventPathNotEventDriven {
        projectId = EventPathProjectNotFound.requireId(projectId, "projectId");
        operationId = EventPathProjectNotFound.requireId(operationId, "operationId");
    }
}
