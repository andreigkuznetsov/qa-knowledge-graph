package ru.kuznetsov.qaip.core.application.query.relationship;

import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.domain.Relationship;

import java.util.ArrayList;
import java.util.Objects;

public final class ProjectRelationshipLookup {
    public ProjectRelationships findByNodeId(Project project, String nodeId) {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(nodeId, "nodeId");
        if (nodeId.isBlank()) throw new IllegalArgumentException("nodeId must not be blank");

        var incoming = new ArrayList<Relationship>();
        var outgoing = new ArrayList<Relationship>();
        for (Relationship relationship : project.relationships()) {
            if (nodeId.equals(relationship.from())) outgoing.add(relationship);
            if (nodeId.equals(relationship.to())) incoming.add(relationship);
        }
        return new ProjectRelationships(incoming, outgoing);
    }
}
