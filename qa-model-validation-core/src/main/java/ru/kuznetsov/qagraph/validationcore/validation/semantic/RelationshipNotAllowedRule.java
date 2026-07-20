package ru.kuznetsov.qagraph.validationcore.validation.semantic;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qagraph.validationcore.model.NodeType;
import ru.kuznetsov.qagraph.validationcore.model.RelationshipType;
import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;
import ru.kuznetsov.qagraph.validationcore.validation.RelationshipRules;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class RelationshipNotAllowedRule implements KnowledgeRule {

    public static final String CODE = "RELATIONSHIP_NOT_ALLOWED";

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
            JsonNode fromNode = nodes.get(
                    SemanticModel.text(relationship, "from"));
            JsonNode toNode = nodes.get(
                    SemanticModel.text(relationship, "to"));
            RelationshipType type = RelationshipType.from(
                    SemanticModel.text(relationship, "type"));
            if (fromNode == null || toNode == null || type == null) {
                continue;
            }
            NodeType fromType = SemanticModel.nodeType(fromNode);
            NodeType toType = SemanticModel.nodeType(toNode);
            if (!RelationshipRules.isAllowed(fromType, type, toType)) {
                findings.add(ValidationIssue.semanticError(
                        CODE,
                        "Недопустимая связь: %s --%s--> %s"
                                .formatted(fromType, type, toType),
                        SemanticModel.text(relationship, "id"),
                        "/relationships/" + index + "/type"));
            }
        }
        return findings;
    }
}
