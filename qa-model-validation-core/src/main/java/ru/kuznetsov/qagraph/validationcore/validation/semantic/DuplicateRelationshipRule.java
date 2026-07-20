package ru.kuznetsov.qagraph.validationcore.validation.semantic;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qagraph.validationcore.model.RelationshipType;
import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DuplicateRelationshipRule implements KnowledgeRule {

    public static final String CODE = "DUPLICATE_RELATIONSHIP";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<ValidationIssue> evaluate(JsonNode model) {
        Map<String, JsonNode> nodes = SemanticModel.nodesById(model);
        Set<String> triples = new HashSet<>();
        JsonNode relationships = model.path("relationships");
        List<ValidationIssue> findings = new ArrayList<>();
        for (int index = 0; index < relationships.size(); index++) {
            JsonNode relationship = relationships.get(index);
            String from = SemanticModel.text(relationship, "from");
            String to = SemanticModel.text(relationship, "to");
            RelationshipType type = RelationshipType.from(
                    SemanticModel.text(relationship, "type"));
            if (!nodes.containsKey(from)
                    || !nodes.containsKey(to)
                    || type == null) {
                continue;
            }
            String triple = from + "|" + type + "|" + to;
            if (!triples.add(triple)) {
                findings.add(ValidationIssue.semanticError(
                        CODE,
                        "Обнаружена дублирующая связь: " + triple,
                        SemanticModel.text(relationship, "id"),
                        "/relationships/" + index));
            }
        }
        return findings;
    }
}
