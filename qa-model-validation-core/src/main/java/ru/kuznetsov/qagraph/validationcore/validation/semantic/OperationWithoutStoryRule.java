package ru.kuznetsov.qagraph.validationcore.validation.semantic;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qagraph.validationcore.model.NodeType;
import ru.kuznetsov.qagraph.validationcore.model.RelationshipType;
import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class OperationWithoutStoryRule implements KnowledgeRule {

    public static final String CODE = "OPERATION_WITHOUT_STORY";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<ValidationIssue> evaluate(JsonNode model) {
        Map<String, Set<RelationshipType>> incoming =
                SemanticModel.relationshipsByEndpoint(model, "to");
        List<ValidationIssue> findings = new ArrayList<>();
        for (JsonNode node : model.path("nodes")) {
            String id = SemanticModel.text(node, "id");
            if (SemanticModel.nodeType(node) == NodeType.BUSINESS_OPERATION
                    && !incoming.getOrDefault(id, Set.of())
                    .contains(RelationshipType.DESCRIBES)) {
                findings.add(ValidationIssue.semanticWarning(CODE,
                        "Бизнес-операция не связана ни с одной User Story", id));
            }
        }
        return findings;
    }
}
