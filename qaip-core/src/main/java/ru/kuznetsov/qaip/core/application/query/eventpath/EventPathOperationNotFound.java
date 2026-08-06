package ru.kuznetsov.qaip.core.application.query.eventpath;

public record EventPathOperationNotFound(String projectId, String operationId) implements EventPathQueryResult {
    public EventPathOperationNotFound {
        projectId = EventPathProjectNotFound.requireId(projectId, "projectId");
        operationId = EventPathProjectNotFound.requireId(operationId, "operationId");
    }
}
