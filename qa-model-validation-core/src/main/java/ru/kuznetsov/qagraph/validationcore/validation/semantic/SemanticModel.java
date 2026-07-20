package ru.kuznetsov.qagraph.validationcore.validation.semantic;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qagraph.validationcore.model.NodeType;
import ru.kuznetsov.qagraph.validationcore.model.RelationshipType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class SemanticModel {

    private SemanticModel() {
    }

    static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    static Map<String, JsonNode> nodesById(JsonNode model) {
        Map<String, JsonNode> nodes = new LinkedHashMap<>();
        for (JsonNode node : model.path("nodes")) {
            nodes.putIfAbsent(text(node, "id"), node);
        }
        return nodes;
    }

    static Map<String, Set<RelationshipType>> relationshipsByEndpoint(
            JsonNode model,
            String endpoint
    ) {
        Map<String, Set<RelationshipType>> result = new HashMap<>();
        for (JsonNode relationship : model.path("relationships")) {
            RelationshipType type = RelationshipType.from(
                    text(relationship, "type"));
            if (type != null) {
                result.computeIfAbsent(text(relationship, endpoint),
                                ignored -> new HashSet<>())
                        .add(type);
            }
        }
        return result;
    }

    static NodeType nodeType(JsonNode node) {
        return NodeType.from(text(node, "type"));
    }
}
