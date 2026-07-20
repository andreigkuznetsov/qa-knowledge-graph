package ru.kuznetsov.qagraph.validationcore.validation.semantic;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DuplicateNodeIdRule implements KnowledgeRule {

    public static final String CODE = "DUPLICATE_NODE_ID";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<ValidationIssue> evaluate(JsonNode model) {
        Set<String> ids = new HashSet<>();
        List<ValidationIssue> findings = new ArrayList<>();
        for (JsonNode node : model.path("nodes")) {
            String id = SemanticModel.text(node, "id");
            if (!ids.add(id)) {
                findings.add(ValidationIssue.semanticError(
                        CODE,
                        "Обнаружен повторяющийся node.id: " + id,
                        id));
            }
        }
        return findings;
    }
}
