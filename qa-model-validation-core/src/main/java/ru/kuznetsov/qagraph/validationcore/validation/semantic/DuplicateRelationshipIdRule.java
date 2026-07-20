package ru.kuznetsov.qagraph.validationcore.validation.semantic;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DuplicateRelationshipIdRule implements KnowledgeRule {

    public static final String CODE = "DUPLICATE_RELATIONSHIP_ID";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<ValidationIssue> evaluate(JsonNode model) {
        Set<String> ids = new HashSet<>();
        List<ValidationIssue> findings = new ArrayList<>();
        JsonNode relationships = model.path("relationships");
        for (int index = 0; index < relationships.size(); index++) {
            JsonNode relationship = relationships.get(index);
            String id = SemanticModel.text(relationship, "id");
            if (!ids.add(id)) {
                findings.add(ValidationIssue.semanticError(
                        CODE,
                        "Обнаружен повторяющийся relationship.id: " + id,
                        id,
                        "/relationships/" + index + "/id"));
            }
        }
        return findings;
    }
}
