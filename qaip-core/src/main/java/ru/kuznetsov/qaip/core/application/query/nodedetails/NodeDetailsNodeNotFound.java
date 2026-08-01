package ru.kuznetsov.qaip.core.application.query.nodedetails;

public record NodeDetailsNodeNotFound(String projectId, String nodeId) implements NodeDetailsQueryResult {
    public NodeDetailsNodeNotFound {
        projectId = NodeDetailsProjectNotFound.requireId(projectId, "projectId");
        nodeId = NodeDetailsProjectNotFound.requireId(nodeId, "nodeId");
    }
}
