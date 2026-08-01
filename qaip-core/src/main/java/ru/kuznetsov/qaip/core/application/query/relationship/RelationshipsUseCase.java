package ru.kuznetsov.qaip.core.application.query.relationship;

public interface RelationshipsUseCase {
    RelationshipsQueryResult execute(String projectId, String nodeId);
}
