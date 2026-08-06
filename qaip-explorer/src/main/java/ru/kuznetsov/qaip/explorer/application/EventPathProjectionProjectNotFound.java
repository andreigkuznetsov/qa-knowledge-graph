package ru.kuznetsov.qaip.explorer.application;

public record EventPathProjectionProjectNotFound(String projectId) implements EventPathProjectionResult {
    public EventPathProjectionProjectNotFound {
        projectId = EventPathProjectionResult.requireId(projectId, "projectId");
    }
}
