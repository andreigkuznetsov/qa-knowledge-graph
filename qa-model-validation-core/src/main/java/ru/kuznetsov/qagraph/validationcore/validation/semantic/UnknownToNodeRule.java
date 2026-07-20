package ru.kuznetsov.qagraph.validationcore.validation.semantic;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class UnknownToNodeRule implements KnowledgeRule {

    public static final String CODE = "UNKNOWN_TO_NODE";

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
            String to = SemanticModel.text(relationship, "to");
            if (!nodes.containsKey(to)) {
                findings.add(ValidationIssue.semanticError(
                        CODE,
                        "Связь указывает на отсутствующий to-узел: " + to,
                        SemanticModel.text(relationship, "id"),
                        "/relationships/" + index + "/to"));
            }
        }
        return findings;
    }
}
