package ru.kuznetsov.qaip.core.application.query.relationship;

import ru.kuznetsov.qaip.core.application.query.node.ProjectNodeLookup;
import ru.kuznetsov.qaip.core.persistence.read.ProjectReader;

import java.util.Objects;

public final class DefaultRelationshipsUseCase implements RelationshipsUseCase {
    private final ProjectReader projectReader;
    private final ProjectNodeLookup projectNodeLookup;
    private final ProjectRelationshipLookup projectRelationshipLookup;
    private final RelationshipDetailsMapper relationshipDetailsMapper;

    public DefaultRelationshipsUseCase(ProjectReader projectReader, ProjectNodeLookup projectNodeLookup,
                                       ProjectRelationshipLookup projectRelationshipLookup,
                                       RelationshipDetailsMapper relationshipDetailsMapper) {
        this.projectReader = Objects.requireNonNull(projectReader, "projectReader");
        this.projectNodeLookup = Objects.requireNonNull(projectNodeLookup, "projectNodeLookup");
        this.projectRelationshipLookup = Objects.requireNonNull(projectRelationshipLookup,
                "projectRelationshipLookup");
        this.relationshipDetailsMapper = Objects.requireNonNull(relationshipDetailsMapper,
                "relationshipDetailsMapper");
    }

    @Override
    public RelationshipsQueryResult execute(String projectId, String nodeId) {
        requireId(projectId, "projectId");
        requireId(nodeId, "nodeId");

        var projectResult = Objects.requireNonNull(projectReader.findById(projectId), "project reader result");
        if (projectResult.isEmpty()) return new RelationshipsProjectNotFound(projectId);

        var project = projectResult.orElseThrow();
        var nodeResult = Objects.requireNonNull(
                projectNodeLookup.findById(project, nodeId), "node lookup result");
        if (nodeResult.isEmpty()) return new RelationshipsNodeNotFound(projectId, nodeId);

        var domainRelationships = Objects.requireNonNull(
                projectRelationshipLookup.findByNodeId(project, nodeId), "relationship lookup result");
        var mappedRelationships = Objects.requireNonNull(
                relationshipDetailsMapper.map(domainRelationships), "relationship details mapper result");
        return new RelationshipsFound(projectId, nodeId, mappedRelationships);
    }

    private static void requireId(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
    }
}
