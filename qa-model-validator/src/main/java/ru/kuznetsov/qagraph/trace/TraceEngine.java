package ru.kuznetsov.qagraph.trace;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import ru.kuznetsov.qagraph.service.QaModelNodeNotFoundException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class TraceEngine {

    public TracePath trace(
            JsonNode model,
            String fromNodeId,
            String toNodeId
    ) {
        GraphIndex graph = buildIndex(model);
        requireNode(graph.nodes(), fromNodeId);
        requireNode(graph.nodes(), toNodeId);

        if (fromNodeId.equals(toNodeId)) {
            return new TracePath(
                    true,
                    List.of(graph.nodes().get(fromNodeId)),
                    List.of()
            );
        }

        ArrayDeque<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        Map<String, TraceRelationship> predecessor = new HashMap<>();

        visited.add(fromNodeId);
        queue.add(fromNodeId);

        while (!queue.isEmpty()) {
            String current = queue.remove();

            for (TraceRelationship relationship :
                    graph.outgoing().getOrDefault(
                            current,
                            List.of()
                    )) {
                if (!visited.add(relationship.to())) {
                    continue;
                }

                predecessor.put(relationship.to(), relationship);

                if (relationship.to().equals(toNodeId)) {
                    return reconstruct(
                            graph.nodes(),
                            predecessor,
                            fromNodeId,
                            toNodeId
                    );
                }

                queue.add(relationship.to());
            }
        }

        return TracePath.notFound();
    }

    private GraphIndex buildIndex(JsonNode model) {
        Map<String, TraceNode> nodes = new LinkedHashMap<>();
        Map<String, List<TraceRelationship>> outgoing =
                new LinkedHashMap<>();

        for (JsonNode node : model.path("nodes")) {
            TraceNode traceNode = new TraceNode(
                    node.path("id").asText(),
                    node.path("type").asText(),
                    node.path("name").asText()
            );
            nodes.put(traceNode.id(), traceNode);
        }

        for (JsonNode relationship : model.path("relationships")) {
            TraceRelationship traceRelationship =
                    new TraceRelationship(
                            relationship.path("type").asText(),
                            relationship.path("from").asText(),
                            relationship.path("to").asText()
                    );
            outgoing.computeIfAbsent(
                            traceRelationship.from(),
                            ignored -> new ArrayList<>()
                    )
                    .add(traceRelationship);
        }

        return new GraphIndex(nodes, outgoing);
    }

    private void requireNode(
            Map<String, TraceNode> nodes,
            String nodeId
    ) {
        if (!nodes.containsKey(nodeId)) {
            throw new QaModelNodeNotFoundException(nodeId);
        }
    }

    private TracePath reconstruct(
            Map<String, TraceNode> nodes,
            Map<String, TraceRelationship> predecessor,
            String fromNodeId,
            String toNodeId
    ) {
        List<TraceNode> pathNodes = new ArrayList<>();
        List<TraceRelationship> pathRelationships =
                new ArrayList<>();
        String current = toNodeId;
        pathNodes.add(nodes.get(current));

        while (!current.equals(fromNodeId)) {
            TraceRelationship relationship = predecessor.get(current);
            pathRelationships.add(relationship);
            current = relationship.from();
            pathNodes.add(nodes.get(current));
        }

        Collections.reverse(pathNodes);
        Collections.reverse(pathRelationships);

        return new TracePath(true, pathNodes, pathRelationships);
    }

    private record GraphIndex(
            Map<String, TraceNode> nodes,
            Map<String, List<TraceRelationship>> outgoing
    ) {
    }
}
