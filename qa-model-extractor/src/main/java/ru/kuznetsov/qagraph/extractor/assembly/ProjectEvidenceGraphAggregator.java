package ru.kuznetsov.qagraph.extractor.assembly;

import ru.kuznetsov.qagraph.extractor.rest.mapping.BusinessOperationProjection;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public final class ProjectEvidenceGraphAggregator {

    public ProjectEvidenceGraphProjection aggregate(Collection<EvidenceGraphProjection> projections) {
        Objects.requireNonNull(projections, "projections");

        Map<String, Object> nodesById = new HashMap<>();
        Map<String, BusinessOperationProjection> operations = new TreeMap<>();
        Map<String, EvidenceGraphProjection.BusinessRuleProjection> rules = new TreeMap<>();
        Map<String, EvidenceGraphProjection.TechnicalImplementationProjection> implementations = new TreeMap<>();
        Map<String, EvidenceGraphProjection.TestImplementationProjection> tests = new TreeMap<>();
        Map<String, EvidenceGraphProjection.CheckProjection> checks = new TreeMap<>();
        Map<String, EvidenceGraphProjection.RelationshipProjection> relationships = new TreeMap<>();

        for (EvidenceGraphProjection projection : projections) {
            Objects.requireNonNull(projection, "projection");
            addNode(projection.businessOperation().id(), projection.businessOperation(), nodesById, operations);
            projection.businessRules().forEach(node -> addNode(node.id(), node, nodesById, rules));
            projection.technicalImplementations().forEach(
                    node -> addNode(node.id(), node, nodesById, implementations));
            projection.testImplementations().forEach(node -> addNode(node.id(), node, nodesById, tests));
            projection.checks().forEach(node -> addNode(node.id(), node, nodesById, checks));
            projection.relationships().forEach(relationship -> addRelationship(relationship, relationships));
        }

        for (var relationship : relationships.values()) {
            if (!nodesById.containsKey(relationship.from()) || !nodesById.containsKey(relationship.to())) {
                throw new IllegalArgumentException(
                        "Relationship " + relationship.id() + " references an unknown endpoint");
            }
        }

        return new ProjectEvidenceGraphProjection(
                operations.values().stream().toList(),
                rules.values().stream().toList(),
                implementations.values().stream().toList(),
                tests.values().stream().toList(),
                checks.values().stream().toList(),
                relationships.values().stream().toList());
    }

    private static <T> void addNode(
            String id,
            T node,
            Map<String, Object> nodesById,
            Map<String, T> nodesOfType
    ) {
        Object existing = nodesById.putIfAbsent(id, node);
        if (existing != null && !existing.equals(node)) {
            throw new IllegalArgumentException("Conflicting node content for identity " + id);
        }
        nodesOfType.putIfAbsent(id, node);
    }

    private static void addRelationship(
            EvidenceGraphProjection.RelationshipProjection relationship,
            Map<String, EvidenceGraphProjection.RelationshipProjection> relationships
    ) {
        var existing = relationships.putIfAbsent(relationship.id(), relationship);
        if (existing != null && !existing.equals(relationship)) {
            throw new IllegalArgumentException(
                    "Conflicting relationship content for identity " + relationship.id());
        }
    }
}
