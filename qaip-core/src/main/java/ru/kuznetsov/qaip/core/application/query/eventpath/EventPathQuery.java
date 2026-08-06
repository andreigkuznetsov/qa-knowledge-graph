package ru.kuznetsov.qaip.core.application.query.eventpath;

public interface EventPathQuery {
    EventPathQueryResult execute(String projectId, String operationId);
}
