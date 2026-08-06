package ru.kuznetsov.qaip.explorer.application;

public interface GetEventPathService {
    EventPathProjectionResult getEventPath(String repositoryId, String operationId);
}
