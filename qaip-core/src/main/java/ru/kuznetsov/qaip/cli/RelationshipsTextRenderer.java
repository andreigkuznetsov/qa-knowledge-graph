package ru.kuznetsov.qaip.cli;

import ru.kuznetsov.qaip.core.application.query.relationship.RelationshipDetails;
import ru.kuznetsov.qaip.core.application.query.relationship.RelationshipDetailsResult;

import java.util.List;
import java.util.Objects;

final class RelationshipsTextRenderer {
    String renderFound(String projectId, String nodeId, RelationshipDetailsResult relationships) {
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(nodeId, "nodeId");
        Objects.requireNonNull(relationships, "relationships");
        return String.join(System.lineSeparator(),
                "Relationships",
                "Project ID: " + projectId,
                "Node ID: " + nodeId,
                "",
                "Incoming:",
                renderDirection(relationships.incoming()),
                "",
                "Outgoing:",
                renderDirection(relationships.outgoing()));
    }

    private static String renderDirection(List<RelationshipDetails> relationships) {
        if (relationships.isEmpty()) return "(none)";
        return String.join(System.lineSeparator(), relationships.stream()
                .map(RelationshipsTextRenderer::renderRelationship)
                .toList());
    }

    private static String renderRelationship(RelationshipDetails relationship) {
        return relationship.relationshipId() + " | " + relationship.relationshipType() + " | "
                + relationship.fromNodeId() + " -> " + relationship.toNodeId();
    }
}
