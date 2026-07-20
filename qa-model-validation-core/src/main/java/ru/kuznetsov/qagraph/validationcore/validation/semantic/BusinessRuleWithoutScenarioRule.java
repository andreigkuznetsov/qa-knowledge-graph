package ru.kuznetsov.qagraph.validationcore.validation.semantic;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qagraph.validationcore.model.NodeType;
import ru.kuznetsov.qagraph.validationcore.model.RelationshipType;
import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BusinessRuleWithoutScenarioRule implements KnowledgeRule {

    public static final String CODE = "RULE_WITHOUT_SCENARIO";

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
            if (SemanticModel.nodeType(node) == NodeType.BUSINESS_RULE
                    && !incoming.getOrDefault(id, Set.of())
                    .contains(RelationshipType.COVERS)) {
                findings.add(ValidationIssue.semanticWarning(
                        CODE,
                        "Бизнес-правило не покрыто BDD-сценарием",
                        id));
            }
        }
        return findings;
    }
}
