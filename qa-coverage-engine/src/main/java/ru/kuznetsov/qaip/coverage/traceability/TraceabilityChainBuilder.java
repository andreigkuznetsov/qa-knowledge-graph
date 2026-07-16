package ru.kuznetsov.qaip.coverage.traceability;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import ru.kuznetsov.qaip.coverage.traceability.model.TraceabilityChain;
import ru.kuznetsov.qaip.coverage.traceability.model.TraceabilityChainBuildResult;
import ru.kuznetsov.qaip.coverage.traceability.model.TraceabilityNodeRef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class TraceabilityChainBuilder {

    public TraceabilityChainBuildResult build(JsonNode qaModel) {
        GraphIndex graph = GraphIndex.from(qaModel);
        ChainCollector collector = new ChainCollector();

        for (TraceabilityNodeRef story : graph.nodesOfType("USER_STORY")) {
            List<TraceabilityNodeRef> operations = graph.outgoing(
                    story.id(), "DESCRIBES", "BUSINESS_OPERATION");
            if (operations.isEmpty()) {
                collector.add(story, null, null, null, null, null);
                continue;
            }
            for (TraceabilityNodeRef operation : operations) {
                appendOperation(graph, collector, story, operation);
            }
        }
        return collector.result();
    }

    private void appendOperation(GraphIndex graph, ChainCollector collector,
            TraceabilityNodeRef story, TraceabilityNodeRef operation) {
        List<TraceabilityNodeRef> rules = graph.outgoing(
                operation.id(), "GOVERNED_BY", "BUSINESS_RULE");
        if (rules.isEmpty()) {
            collector.add(story, operation, null, null, null, null);
            return;
        }
        for (TraceabilityNodeRef rule : rules) {
            appendRule(graph, collector, story, operation, rule);
        }
    }

    private void appendRule(GraphIndex graph, ChainCollector collector,
            TraceabilityNodeRef story, TraceabilityNodeRef operation,
            TraceabilityNodeRef rule) {
        List<TraceabilityNodeRef> scenarios = graph.incoming(
                rule.id(), "COVERS", "SCENARIO");
        if (scenarios.isEmpty()) {
            collector.add(story, operation, rule, null, null, null);
            return;
        }
        for (TraceabilityNodeRef scenario : scenarios) {
            appendScenario(graph, collector, story, operation, rule, scenario);
        }
    }

    private void appendScenario(GraphIndex graph, ChainCollector collector,
            TraceabilityNodeRef story, TraceabilityNodeRef operation,
            TraceabilityNodeRef rule, TraceabilityNodeRef scenario) {
        List<TraceabilityNodeRef> tests = graph.incoming(
                scenario.id(), "VALIDATES", "TEST_IMPLEMENTATION");
        if (tests.isEmpty()) {
            collector.add(story, operation, rule, scenario, null, null);
            return;
        }
        for (TraceabilityNodeRef test : tests) {
            appendTest(graph, collector, story, operation, rule, scenario, test);
        }
    }

    private void appendTest(GraphIndex graph, ChainCollector collector,
            TraceabilityNodeRef story, TraceabilityNodeRef operation,
            TraceabilityNodeRef rule, TraceabilityNodeRef scenario,
            TraceabilityNodeRef test) {
        List<TraceabilityNodeRef> checks = graph.outgoing(
                test.id(), "HAS_CHECK", "CHECK");
        if (checks.isEmpty()) {
            collector.add(story, operation, rule, scenario, test, null);
            return;
        }
        for (TraceabilityNodeRef check : checks) {
            collector.add(story, operation, rule, scenario, test, check);
        }
    }

    private static final class ChainCollector {
        private final List<TraceabilityChain> chains = new ArrayList<>();

        void add(TraceabilityNodeRef story, TraceabilityNodeRef operation,
                TraceabilityNodeRef rule, TraceabilityNodeRef scenario,
                TraceabilityNodeRef test, TraceabilityNodeRef check) {
            List<TraceabilityNodeRef> nodes = Stream.of(
                    story, operation, rule, scenario, test, check)
                    .filter(node -> node != null).toList();
            TraceabilityNodeRef lastNode = nodes.getLast();
            chains.add(new TraceabilityChain(
                    "CHAIN-%03d".formatted(chains.size() + 1),
                    story, operation, rule, scenario, test, check,
                    lastNode,
                    nodes.stream().map(TraceabilityNodeRef::id).toList(),
                    nodes.size()));
        }

        TraceabilityChainBuildResult result() {
            return new TraceabilityChainBuildResult(
                    chains.size(), List.copyOf(chains));
        }
    }

    private static final class GraphIndex {
        private final Map<String, TraceabilityNodeRef> nodes;
        private final Map<String, List<Edge>> outgoing;
        private final Map<String, List<Edge>> incoming;

        private GraphIndex(Map<String, TraceabilityNodeRef> nodes,
                Map<String, List<Edge>> outgoing,
                Map<String, List<Edge>> incoming) {
            this.nodes = nodes; this.outgoing = outgoing; this.incoming = incoming;
        }

        static GraphIndex from(JsonNode qaModel) {
            Map<String, TraceabilityNodeRef> nodes = new HashMap<>();
            Map<String, List<Edge>> outgoing = new HashMap<>();
            Map<String, List<Edge>> incoming = new HashMap<>();
            JsonNode nodeArray = qaModel.path("nodes");
            for (int i = 0; i < nodeArray.size(); i++) {
                JsonNode node = nodeArray.get(i);
                String id = text(node, "id");
                nodes.put(id, new TraceabilityNodeRef(
                        id, text(node, "type"), text(node, "name"), "/nodes/" + i));
            }
            for (JsonNode rel : qaModel.path("relationships")) {
                Edge edge = new Edge(text(rel,"from"), text(rel,"type"), text(rel,"to"));
                outgoing.computeIfAbsent(edge.from(), key -> new ArrayList<>()).add(edge);
                incoming.computeIfAbsent(edge.to(), key -> new ArrayList<>()).add(edge);
            }
            return new GraphIndex(nodes, outgoing, incoming);
        }

        List<TraceabilityNodeRef> nodesOfType(String type) {
            return nodes.values().stream().filter(n -> type.equals(n.type()))
                    .sorted((a,b) -> a.id().compareTo(b.id())).toList();
        }

        List<TraceabilityNodeRef> outgoing(String from, String relType, String nodeType) {
            return outgoing.getOrDefault(from, List.of()).stream()
                    .filter(e -> relType.equals(e.type())).map(e -> nodes.get(e.to()))
                    .filter(n -> n != null && nodeType.equals(n.type()))
                    .sorted((a,b) -> a.id().compareTo(b.id())).toList();
        }

        List<TraceabilityNodeRef> incoming(String to, String relType, String nodeType) {
            return incoming.getOrDefault(to, List.of()).stream()
                    .filter(e -> relType.equals(e.type())).map(e -> nodes.get(e.from()))
                    .filter(n -> n != null && nodeType.equals(n.type()))
                    .sorted((a,b) -> a.id().compareTo(b.id())).toList();
        }

        private static String text(JsonNode node, String field) {
            JsonNode value = node.get(field);
            return value == null || value.isNull() ? null : value.asText();
        }

        private record Edge(String from, String type, String to) {}
    }
}
