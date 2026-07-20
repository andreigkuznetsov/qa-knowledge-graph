package ru.kuznetsov.qagraph.validationcore.validation.semantic;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qagraph.validationcore.model.NodeType;
import ru.kuznetsov.qagraph.validationcore.model.RelationshipType;
import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BusinessOperationCoverageRule implements KnowledgeRule {

    public static final String CODE = "BUSINESS_OPERATION_RELATIONSHIPS";

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
            if (SemanticModel.nodeType(node) != NodeType.BUSINESS_OPERATION) {
                continue;
            }
            String id = SemanticModel.text(node, "id");
            Set<RelationshipType> out = outgoing.getOrDefault(id, Set.of());
            Set<RelationshipType> in = incoming.getOrDefault(id, Set.of());
            warnMissing(out, RelationshipType.GOVERNED_BY,
                    "OPERATION_WITHOUT_RULE",
                    "Бизнес-операция не связана ни с одним правилом",
                    id, findings);
            warnMissing(out, RelationshipType.SPECIFIED_BY,
                    "OPERATION_WITHOUT_SCENARIO",
                    "Бизнес-операция не связана ни с одним сценарием",
                    id, findings);
            warnMissing(out, RelationshipType.IMPLEMENTED_BY,
                    "OPERATION_WITHOUT_IMPLEMENTATION",
                    "Бизнес-операция не имеет технической реализации",
                    id, findings);
            warnMissing(in, RelationshipType.DESCRIBES,
                    "OPERATION_WITHOUT_STORY",
                    "Бизнес-операция не связана ни с одной User Story",
                    id, findings);
        }
        return findings;
    }

    private void warnMissing(
            Set<RelationshipType> available,
            RelationshipType expected,
            String code,
            String message,
            String objectId,
            List<ValidationIssue> findings
    ) {
        if (!available.contains(expected)) {
            findings.add(ValidationIssue.semanticWarning(
                    code, message, objectId));
        }
    }
}
