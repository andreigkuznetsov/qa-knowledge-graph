package ru.kuznetsov.qaip.core.application.query.eventpath;

public record EventPathAmbiguous(String projectId, String operationId) implements EventPathQueryResult {
    public EventPathAmbiguous {
        projectId = EventPathProjectNotFound.requireId(projectId, "projectId");
        operationId = EventPathProjectNotFound.requireId(operationId, "operationId");
    }
}
