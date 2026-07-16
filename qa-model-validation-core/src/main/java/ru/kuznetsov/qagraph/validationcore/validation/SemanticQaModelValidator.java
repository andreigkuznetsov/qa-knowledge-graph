package ru.kuznetsov.qagraph.validationcore.validation;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qagraph.validationcore.model.NodeType;
import ru.kuznetsov.qagraph.validationcore.model.RelationshipType;
import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;

import java.util.*;

public class SemanticQaModelValidator {

    public List<ValidationIssue> validate(JsonNode document) {
        List<ValidationIssue> issues = new ArrayList<>();

        Map<String, JsonNode> nodesById = indexNodes(document.path("nodes"), issues);
        Set<String> sourceIds = indexIds(document.path("sources"), "id");

        validateSourceReferences(document.path("nodes"), sourceIds, issues);
        validateRelationships(document.path("relationships"), nodesById, issues);
        validateTestStepOrder(document.path("nodes"), issues);
        validateCoverage(document.path("nodes"), document.path("relationships"), issues);

        return issues;
    }

    private Map<String, JsonNode> indexNodes(JsonNode nodes, List<ValidationIssue> issues) {
        Map<String, JsonNode> result = new LinkedHashMap<>();
        for (JsonNode node : nodes) {
            String id = text(node, "id");
            if (result.putIfAbsent(id, node) != null) {
                issues.add(ValidationIssue.semanticError(
                        "DUPLICATE_NODE_ID",
                        "Обнаружен повторяющийся node.id: " + id,
                        id
                ));
            }
        }
        return result;
    }

    private Set<String> indexIds(JsonNode array, String field) {
        Set<String> result = new HashSet<>();
        for (JsonNode item : array) {
            result.add(text(item, field));
        }
        return result;
    }

    private void validateSourceReferences(
            JsonNode nodes,
            Set<String> sourceIds,
            List<ValidationIssue> issues
    ) {
        for (JsonNode node : nodes) {
            String nodeId = text(node, "id");
            JsonNode references = node.path("sourceReferences");

            if ("CONFIRMED".equals(text(node, "status")) && references.isEmpty()) {
                issues.add(ValidationIssue.semanticWarning(
                        "CONFIRMED_WITHOUT_SOURCE",
                        "Подтверждённый узел не имеет ссылки на источник",
                        nodeId
                ));
            }

            for (JsonNode reference : references) {
                String sourceId = text(reference, "sourceId");
                if (!sourceIds.contains(sourceId)) {
                    issues.add(ValidationIssue.semanticError(
                            "UNKNOWN_SOURCE_REFERENCE",
                            "Ссылка указывает на отсутствующий источник: " + sourceId,
                            nodeId
                    ));
                }
            }
        }
    }

    private void validateRelationships(
            JsonNode relationships,
            Map<String, JsonNode> nodesById,
            List<ValidationIssue> issues
    ) {
        Set<String> relationshipIds = new HashSet<>();
        Set<String> triples = new HashSet<>();

        for (int relationshipIndex = 0;
             relationshipIndex < relationships.size();
             relationshipIndex++) {

            JsonNode relationship =
                    relationships.get(relationshipIndex);

            String relationshipPath =
                    "/relationships/" + relationshipIndex;

            String id = text(relationship, "id");
            String from = text(relationship, "from");
            String to = text(relationship, "to");

            RelationshipType relationshipType =
                    RelationshipType.from(
                            text(relationship, "type")
                    );

            if (!relationshipIds.add(id)) {
                issues.add(ValidationIssue.semanticError(
                        "DUPLICATE_RELATIONSHIP_ID",
                        "Обнаружен повторяющийся relationship.id: " + id,
                        id,
                        relationshipPath + "/id"
                ));
            }

            JsonNode fromNode = nodesById.get(from);
            JsonNode toNode = nodesById.get(to);

            if (fromNode == null) {
                issues.add(ValidationIssue.semanticError(
                        "UNKNOWN_FROM_NODE",
                        "Связь указывает на отсутствующий from-узел: "
                                + from,
                        id,
                        relationshipPath + "/from"
                ));
            }

            if (toNode == null) {
                issues.add(ValidationIssue.semanticError(
                        "UNKNOWN_TO_NODE",
                        "Связь указывает на отсутствующий to-узел: "
                                + to,
                        id,
                        relationshipPath + "/to"
                ));
            }

            if (fromNode == null
                    || toNode == null
                    || relationshipType == null) {
                continue;
            }

            NodeType fromType =
                    NodeType.from(text(fromNode, "type"));

            NodeType toType =
                    NodeType.from(text(toNode, "type"));

            if (!RelationshipRules.isAllowed(
                    fromType,
                    relationshipType,
                    toType
            )) {
                issues.add(ValidationIssue.semanticError(
                        "RELATIONSHIP_NOT_ALLOWED",
                        "Недопустимая связь: %s --%s--> %s"
                                .formatted(
                                        fromType,
                                        relationshipType,
                                        toType
                                ),
                        id,
                        relationshipPath + "/type"
                ));
            }

            if (from.equals(to)
                    && !RelationshipRules.isSelfReferenceAllowed(
                    fromType,
                    relationshipType,
                    toType
            )) {
                issues.add(ValidationIssue.semanticError(
                        "SELF_REFERENCE_NOT_ALLOWED",
                        "Самоссылка запрещена для связи "
                                + relationshipType,
                        id,
                        relationshipPath
                ));
            }

            String triple =
                    from + "|" + relationshipType + "|" + to;

            if (!triples.add(triple)) {
                issues.add(ValidationIssue.semanticError(
                        "DUPLICATE_RELATIONSHIP",
                        "Обнаружена дублирующая связь: " + triple,
                        id,
                        relationshipPath
                ));
            }
        }
    }

    private void validateTestStepOrder(JsonNode nodes, List<ValidationIssue> issues) {
        for (JsonNode node : nodes) {
            if (!"TEST_IMPLEMENTATION".equals(text(node, "type"))) {
                continue;
            }

            Set<Integer> orders = new HashSet<>();
            for (JsonNode step : node.path("testImplementation").path("steps")) {
                int order = step.path("order").asInt();
                if (!orders.add(order)) {
                    issues.add(ValidationIssue.semanticError(
                            "DUPLICATE_TEST_STEP_ORDER",
                            "В тесте повторяется номер шага: " + order,
                            text(node, "id")
                    ));
                }
            }
        }
    }

    private void validateCoverage(
            JsonNode nodes,
            JsonNode relationships,
            List<ValidationIssue> issues
    ) {
        Map<String, Set<RelationshipType>> outgoing = new HashMap<>();
        Map<String, Set<RelationshipType>> incoming = new HashMap<>();

        for (JsonNode relationship : relationships) {
            RelationshipType type = RelationshipType.from(text(relationship, "type"));
            if (type == null) {
                continue;
            }
            outgoing.computeIfAbsent(text(relationship, "from"), ignored -> new HashSet<>()).add(type);
            incoming.computeIfAbsent(text(relationship, "to"), ignored -> new HashSet<>()).add(type);
        }

        for (JsonNode node : nodes) {
            String id = text(node, "id");
            NodeType type = NodeType.from(text(node, "type"));
            Set<RelationshipType> out = outgoing.getOrDefault(id, Set.of());
            Set<RelationshipType> in = incoming.getOrDefault(id, Set.of());

            if (type == NodeType.BUSINESS_OPERATION) {
                warnMissing(out, RelationshipType.GOVERNED_BY,
                        "OPERATION_WITHOUT_RULE", "Бизнес-операция не связана ни с одним правилом", id, issues);
                warnMissing(out, RelationshipType.SPECIFIED_BY,
                        "OPERATION_WITHOUT_SCENARIO", "Бизнес-операция не связана ни с одним сценарием", id, issues);
                warnMissing(out, RelationshipType.IMPLEMENTED_BY,
                        "OPERATION_WITHOUT_IMPLEMENTATION", "Бизнес-операция не имеет технической реализации", id, issues);
                warnMissing(in, RelationshipType.DESCRIBES,
                        "OPERATION_WITHOUT_STORY", "Бизнес-операция не связана ни с одной User Story", id, issues);
            }

            if (type == NodeType.SCENARIO && !in.contains(RelationshipType.VALIDATES)) {
                issues.add(ValidationIssue.semanticWarning(
                        "SCENARIO_WITHOUT_TEST",
                        "BDD-сценарий не покрыт тестовой реализацией",
                        id
                ));
            }

            if (type == NodeType.BUSINESS_RULE && !in.contains(RelationshipType.COVERS)) {
                issues.add(ValidationIssue.semanticWarning(
                        "RULE_WITHOUT_SCENARIO",
                        "Бизнес-правило не покрыто BDD-сценарием",
                        id
                ));
            }

            if (type == NodeType.TEST_IMPLEMENTATION && !out.contains(RelationshipType.HAS_CHECK)) {
                issues.add(ValidationIssue.semanticWarning(
                        "TEST_WITHOUT_CHECK",
                        "Тестовая реализация не содержит проверок",
                        id
                ));
            }

            if (type == NodeType.CHECK && !in.contains(RelationshipType.HAS_CHECK)) {
                issues.add(ValidationIssue.semanticWarning(
                        "ORPHAN_CHECK",
                        "Проверка не привязана к тестовой реализации",
                        id
                ));
            }
        }
    }

    private void warnMissing(
            Set<RelationshipType> available,
            RelationshipType expected,
            String code,
            String message,
            String objectId,
            List<ValidationIssue> issues
    ) {
        if (!available.contains(expected)) {
            issues.add(ValidationIssue.semanticWarning(code, message, objectId));
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
