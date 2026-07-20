package ru.kuznetsov.qagraph.validationcore.validation.semantic;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qagraph.validationcore.model.NodeType;
import ru.kuznetsov.qagraph.validationcore.model.RelationshipType;
import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;
import ru.kuznetsov.qagraph.validationcore.validation.RelationshipRules;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SelfReferenceNotAllowedRule implements KnowledgeRule {

    public static final String CODE = "SELF_REFERENCE_NOT_ALLOWED";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<ValidationIssue> evaluate(JsonNode model) {
        Map<String, JsonNode> nodes = SemanticModel.nodesById(model);
        JsonNode relationships = model.path("relationships");
        List<ValidationIssue> findings = new ArrayList<>();
        for (int index = 0; index < relationships.size(); index++) {
            JsonNode relationship = relationships.get(index);
            String from = SemanticModel.text(relationship, "from");
            String to = SemanticModel.text(relationship, "to");
            JsonNode fromNode = nodes.get(from);
            JsonNode toNode = nodes.get(to);
            RelationshipType type = RelationshipType.from(
                    SemanticModel.text(relationship, "type"));
            if (fromNode == null || toNode == null || type == null) {
                continue;
            }
            NodeType fromType = SemanticModel.nodeType(fromNode);
            NodeType toType = SemanticModel.nodeType(toNode);
            if (from.equals(to)
                    && !RelationshipRules.isSelfReferenceAllowed(
                    fromType, type, toType)) {
                findings.add(ValidationIssue.semanticError(
                        CODE,
                        "Самоссылка запрещена для связи " + type,
                        SemanticModel.text(relationship, "id"),
                        "/relationships/" + index));
            }
        }
        return findings;
    }
}
