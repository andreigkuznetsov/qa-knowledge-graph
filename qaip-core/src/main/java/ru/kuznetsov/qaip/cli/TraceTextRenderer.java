package ru.kuznetsov.qaip.cli;

import ru.kuznetsov.qaip.core.application.query.trace.TraceNode;
import ru.kuznetsov.qaip.core.application.query.trace.TraceRelationship;
import ru.kuznetsov.qaip.core.application.query.trace.TraceResult;

import java.util.List;
import java.util.Objects;

final class TraceTextRenderer {
    String renderFound(String projectId, String startNodeId, TraceResult trace) {
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(startNodeId, "startNodeId");
        Objects.requireNonNull(trace, "trace");
        return String.join(System.lineSeparator(),
                "Trace",
                "Project ID: " + projectId,
                "Start Node ID: " + startNodeId,
                "",
                "Nodes:",
                renderNodes(trace.nodes()),
                "",
                "Relationships:",
                renderRelationships(trace.relationships()));
    }

    private static String renderNodes(List<TraceNode> nodes) {
        return String.join(System.lineSeparator(), nodes.stream()
                .map(node -> node.nodeId() + " | " + node.nodeType())
                .toList());
    }

    private static String renderRelationships(List<TraceRelationship> relationships) {
        if (relationships.isEmpty()) return "(none)";
        return String.join(System.lineSeparator(), relationships.stream()
                .map(relationship -> relationship.relationshipId() + " | " + relationship.relationshipType()
                        + " | " + relationship.fromNodeId() + " -> " + relationship.toNodeId())
                .toList());
    }
}
