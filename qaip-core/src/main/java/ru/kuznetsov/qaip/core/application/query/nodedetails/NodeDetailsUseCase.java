package ru.kuznetsov.qaip.core.application.query.nodedetails;

public interface NodeDetailsUseCase {
    NodeDetailsQueryResult execute(String projectId, String nodeId);
}
