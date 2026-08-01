package ru.kuznetsov.qaip.core.application.query.nodedetails;

import ru.kuznetsov.qaip.core.application.query.node.ProjectNodeLookup;
import ru.kuznetsov.qaip.core.persistence.read.ProjectReader;

import java.util.Objects;

public final class DefaultNodeDetailsUseCase implements NodeDetailsUseCase {
    private final ProjectReader projectReader;
    private final ProjectNodeLookup projectNodeLookup;
    private final NodeDetailsMapper nodeDetailsMapper;

    public DefaultNodeDetailsUseCase(ProjectReader projectReader, ProjectNodeLookup projectNodeLookup,
                                     NodeDetailsMapper nodeDetailsMapper) {
        this.projectReader = Objects.requireNonNull(projectReader, "projectReader");
        this.projectNodeLookup = Objects.requireNonNull(projectNodeLookup, "projectNodeLookup");
        this.nodeDetailsMapper = Objects.requireNonNull(nodeDetailsMapper, "nodeDetailsMapper");
    }

    @Override
    public NodeDetailsQueryResult execute(String projectId, String nodeId) {
        requireId(projectId, "projectId");
        requireId(nodeId, "nodeId");

        var project = Objects.requireNonNull(projectReader.findById(projectId), "project reader result");
        if (project.isEmpty()) return new NodeDetailsProjectNotFound(projectId);

        var node = Objects.requireNonNull(
                projectNodeLookup.findById(project.orElseThrow(), nodeId), "node lookup result");
        if (node.isEmpty()) return new NodeDetailsNodeNotFound(projectId, nodeId);

        var details = Objects.requireNonNull(
                nodeDetailsMapper.map(node.orElseThrow()), "node details mapper result");
        return new NodeDetailsFound(details);
    }

    private static void requireId(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
    }
}
