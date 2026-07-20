package ru.kuznetsov.qagraph.validationcore.validation.semantic;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qagraph.validationcore.model.NodeType;
import ru.kuznetsov.qagraph.validationcore.model.RelationshipType;
import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;
import ru.kuznetsov.qagraph.validationcore.validation.RelationshipRules;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RelationshipIntegrityRule implements KnowledgeRule {

    public static final String CODE = "RELATIONSHIP_INTEGRITY";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<ValidationIssue> evaluate(JsonNode model) {
        Map<String, JsonNode> nodes = SemanticModel.nodesById(model);
        Set<String> ids = new HashSet<>();
        Set<String> triples = new HashSet<>();
        List<ValidationIssue> findings = new ArrayList<>();
        JsonNode relationships = model.path("relationships");

        for (int index = 0; index < relationships.size(); index++) {
            JsonNode relationship = relationships.get(index);
            String path = "/relationships/" + index;
            String id = SemanticModel.text(relationship, "id");
            String from = SemanticModel.text(relationship, "from");
            String to = SemanticModel.text(relationship, "to");
            RelationshipType type = RelationshipType.from(
                    SemanticModel.text(relationship, "type"));

            if (!ids.add(id)) {
                findings.add(ValidationIssue.semanticError(
                        "DUPLICATE_RELATIONSHIP_ID",
                        "Обнаружен повторяющийся relationship.id: " + id,
                        id,
                        path + "/id"));
            }

            JsonNode fromNode = nodes.get(from);
            JsonNode toNode = nodes.get(to);
            if (fromNode == null) {
                findings.add(ValidationIssue.semanticError(
                        "UNKNOWN_FROM_NODE",
                        "Связь указывает на отсутствующий from-узел: " + from,
                        id,
                        path + "/from"));
            }

            if (fromNode == null || toNode == null || type == null) {
                continue;
            }
            NodeType fromType = SemanticModel.nodeType(fromNode);
            NodeType toType = SemanticModel.nodeType(toNode);
            if (from.equals(to)
                    && !RelationshipRules.isSelfReferenceAllowed(
                    fromType, type, toType)) {
                findings.add(ValidationIssue.semanticError(
                        "SELF_REFERENCE_NOT_ALLOWED",
                        "Самоссылка запрещена для связи " + type,
                        id,
                        path));
            }

            String triple = from + "|" + type + "|" + to;
            if (!triples.add(triple)) {
                findings.add(ValidationIssue.semanticError(
                        "DUPLICATE_RELATIONSHIP",
                        "Обнаружена дублирующая связь: " + triple,
                        id,
                        path));
            }
        }
        return findings;
    }
}
