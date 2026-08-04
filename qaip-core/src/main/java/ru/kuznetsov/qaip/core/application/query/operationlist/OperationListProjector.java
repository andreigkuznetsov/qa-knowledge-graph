package ru.kuznetsov.qaip.core.application.query.operationlist;

import ru.kuznetsov.qaip.core.domain.Node;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.domain.Relationship;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Projects operation verification inputs from the canonical relationship graph in one place. */
public final class OperationListProjector {
    private static final String BUSINESS_OPERATION = "BUSINESS_OPERATION";
    private static final String IMPLEMENTED_BY = "IMPLEMENTED_BY";
    private static final String SPECIFIED_BY = "SPECIFIED_BY";
    private static final String USES = "USES";
    private static final String VALIDATES = "VALIDATES";
    private static final String HAS_CHECK = "HAS_CHECK";

    public List<OperationQueryResult> project(Project project) {
        Objects.requireNonNull(project, "project");
        RelationshipIndex relationships = new RelationshipIndex(project.relationships());
        List<OperationQueryResult> operations = new ArrayList<>();
        for (Node node : project.nodes()) {
            if (!BUSINESS_OPERATION.equals(node.type())) continue;
            Endpoint endpoint = Endpoint.from(node);
            Set<String> tests = relatedTests(node.id(), relationships);
            Set<String> checks = new HashSet<>();
            tests.forEach(testId -> checks.addAll(relationships.targets(testId, HAS_CHECK)));
            operations.add(new OperationQueryResult(
                    node.id(), endpoint.method(), endpoint.path(), node.name(), tests.size(), checks.size()));
        }
        operations.sort(Comparator.comparing(OperationQueryResult::method)
                .thenComparing(OperationQueryResult::path)
                .thenComparing(OperationQueryResult::operationId));
        return List.copyOf(operations);
    }

    private static Set<String> relatedTests(String operationId, RelationshipIndex relationships) {
        Set<String> tests = new HashSet<>();
        for (String implementationId : relationships.targets(operationId, IMPLEMENTED_BY)) {
            tests.addAll(relationships.sources(implementationId, USES));
        }
        for (String scenarioId : relationships.targets(operationId, SPECIFIED_BY)) {
            tests.addAll(relationships.sources(scenarioId, VALIDATES));
        }
        return tests;
    }

    private record Endpoint(String method, String path) {
        private static Endpoint from(Node node) {
            String[] parts = Objects.requireNonNull(node.name(), "business operation name").trim().split("\\s+", 2);
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                throw new IllegalArgumentException(
                        "business operation name must contain HTTP method and path: " + node.id());
            }
            return new Endpoint(parts[0], parts[1]);
        }
    }

    private static final class RelationshipIndex {
        private final Map<Key, Set<String>> targets = new HashMap<>();
        private final Map<Key, Set<String>> sources = new HashMap<>();

        private RelationshipIndex(List<Relationship> relationships) {
            Objects.requireNonNull(relationships, "relationships");
            for (Relationship relationship : relationships) {
                targets.computeIfAbsent(new Key(relationship.from(), relationship.type()), ignored -> new HashSet<>())
                        .add(relationship.to());
                sources.computeIfAbsent(new Key(relationship.to(), relationship.type()), ignored -> new HashSet<>())
                        .add(relationship.from());
            }
        }

        private Set<String> targets(String sourceId, String type) {
            return targets.getOrDefault(new Key(sourceId, type), Set.of());
        }

        private Set<String> sources(String targetId, String type) {
            return sources.getOrDefault(new Key(targetId, type), Set.of());
        }

        private record Key(String nodeId, String type) { }
    }
}
