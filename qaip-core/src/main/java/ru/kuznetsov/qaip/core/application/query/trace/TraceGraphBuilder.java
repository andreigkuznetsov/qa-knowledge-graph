package ru.kuznetsov.qaip.core.application.query.trace;

import ru.kuznetsov.qaip.core.domain.Node;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.domain.Relationship;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class TraceGraphBuilder {
    public TraceGraph build(Project project, String startNodeId) {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(startNodeId, "startNodeId");
        if (startNodeId.isBlank()) throw new IllegalArgumentException("startNodeId must not be blank");

        Map<String, Node> nodesById = new HashMap<>();
        for (Node node : project.nodes()) nodesById.putIfAbsent(node.id(), node);
        Node start = nodesById.get(startNodeId);
        if (start == null) throw new IllegalArgumentException("start Node not found: " + startNodeId);

        List<Relationship> projectRelationships = project.relationships();
        Map<String, List<Integer>> adjacency = new HashMap<>();
        for (int index = 0; index < projectRelationships.size(); index++) {
            Relationship relationship = projectRelationships.get(index);
            List<Integer> fromRelationships = adjacency.get(relationship.from());
            if (fromRelationships == null) {
                fromRelationships = new ArrayList<>();
                adjacency.put(relationship.from(), fromRelationships);
            }
            fromRelationships.add(index);
            if (!relationship.from().equals(relationship.to())) {
                List<Integer> toRelationships = adjacency.get(relationship.to());
                if (toRelationships == null) {
                    toRelationships = new ArrayList<>();
                    adjacency.put(relationship.to(), toRelationships);
                }
                toRelationships.add(index);
            }
        }

        var queue = new ArrayDeque<Node>();
        var visitedNodeIds = new HashSet<String>();
        var includedRelationshipIds = new HashSet<String>();
        var includedRelationshipIndexes = new HashSet<Integer>();
        var resultNodes = new ArrayList<Node>();
        visitedNodeIds.add(startNodeId);
        queue.addLast(start);

        while (!queue.isEmpty()) {
            Node current = queue.removeFirst();
            resultNodes.add(current);
            for (int index : adjacency.getOrDefault(current.id(), List.of())) {
                Relationship relationship = projectRelationships.get(index);
                if (includedRelationshipIds.add(relationship.id())) includedRelationshipIndexes.add(index);
                String oppositeId = current.id().equals(relationship.from())
                        ? relationship.to()
                        : relationship.from();
                Node opposite = nodesById.get(oppositeId);
                if (opposite == null) {
                    throw new IllegalStateException("relationship endpoint Node not found: " + oppositeId);
                }
                if (visitedNodeIds.add(oppositeId)) queue.addLast(opposite);
            }
        }

        var resultRelationships = new ArrayList<Relationship>();
        for (int index = 0; index < projectRelationships.size(); index++) {
            if (includedRelationshipIndexes.contains(index)) resultRelationships.add(projectRelationships.get(index));
        }
        return new TraceGraph(startNodeId, resultNodes, resultRelationships);
    }
}
