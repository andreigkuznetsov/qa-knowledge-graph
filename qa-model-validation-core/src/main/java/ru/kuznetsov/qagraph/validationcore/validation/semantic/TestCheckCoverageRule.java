package ru.kuznetsov.qagraph.validationcore.validation.semantic;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qagraph.validationcore.model.NodeType;
import ru.kuznetsov.qagraph.validationcore.model.RelationshipType;
import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TestCheckCoverageRule implements KnowledgeRule {

    public static final String CODE = "TEST_CHECK_ASSOCIATION";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<ValidationIssue> evaluate(JsonNode model) {
        Map<String, Set<RelationshipType>> outgoing =
                SemanticModel.relationshipsByEndpoint(model, "from");
        Map<String, Set<RelationshipType>> incoming =
                SemanticModel.relationshipsByEndpoint(model, "to");
        List<ValidationIssue> findings = new ArrayList<>();
        for (JsonNode node : model.path("nodes")) {
            String id = SemanticModel.text(node, "id");
            NodeType type = SemanticModel.nodeType(node);
            if (type == NodeType.TEST_IMPLEMENTATION
                    && !outgoing.getOrDefault(id, Set.of())
                    .contains(RelationshipType.HAS_CHECK)) {
                findings.add(ValidationIssue.semanticWarning(
                        "TEST_WITHOUT_CHECK",
                        "Тестовая реализация не содержит проверок",
                        id));
            }
            if (type == NodeType.CHECK
                    && !incoming.getOrDefault(id, Set.of())
                    .contains(RelationshipType.HAS_CHECK)) {
                findings.add(ValidationIssue.semanticWarning(
                        "ORPHAN_CHECK",
                        "Проверка не привязана к тестовой реализации",
                        id));
            }
        }
        return findings;
    }
}
