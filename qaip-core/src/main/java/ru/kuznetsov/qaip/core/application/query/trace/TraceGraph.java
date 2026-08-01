package ru.kuznetsov.qaip.core.application.query.trace;

import ru.kuznetsov.qaip.core.domain.Node;
import ru.kuznetsov.qaip.core.domain.Relationship;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record TraceGraph(
        String startNodeId,
        List<Node> nodes,
        List<Relationship> relationships
) {
    public TraceGraph {
        Objects.requireNonNull(startNodeId, "startNodeId");
        if (startNodeId.isBlank()) throw new IllegalArgumentException("startNodeId must not be blank");
        nodes = List.copyOf(nodes);
        relationships = List.copyOf(relationships);

        var nodeIds = new HashSet<String>();
        boolean containsStart = false;
        for (Node node : nodes) {
            if (!nodeIds.add(node.id())) {
                throw new IllegalArgumentException("duplicate Node ID: " + node.id());
            }
            if (startNodeId.equals(node.id())) containsStart = true;
        }
        if (!containsStart) throw new IllegalArgumentException("nodes must contain start Node: " + startNodeId);

        var relationshipIds = new HashSet<String>();
        for (Relationship relationship : relationships) {
            if (!relationshipIds.add(relationship.id())) {
                throw new IllegalArgumentException("duplicate Relationship ID: " + relationship.id());
            }
        }
    }
}
