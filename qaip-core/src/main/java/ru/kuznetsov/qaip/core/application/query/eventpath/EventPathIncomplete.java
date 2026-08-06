package ru.kuznetsov.qaip.core.application.query.eventpath;

public record EventPathIncomplete(String projectId, String operationId) implements EventPathQueryResult {
    public EventPathIncomplete {
        projectId = EventPathProjectNotFound.requireId(projectId, "projectId");
        operationId = EventPathProjectNotFound.requireId(operationId, "operationId");
    }
}
