package ru.kuznetsov.qagraph.validationcore.validation.semantic;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SourceReferenceRule implements KnowledgeRule {

    public static final String CODE = "SOURCE_REFERENCE_VALIDITY";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<ValidationIssue> evaluate(JsonNode model) {
        Set<String> sourceIds = new HashSet<>();
        for (JsonNode source : model.path("sources")) {
            sourceIds.add(SemanticModel.text(source, "id"));
        }

        List<ValidationIssue> findings = new ArrayList<>();
        for (JsonNode node : model.path("nodes")) {
            String nodeId = SemanticModel.text(node, "id");
            JsonNode references = node.path("sourceReferences");
            if ("CONFIRMED".equals(SemanticModel.text(node, "status"))
                    && references.isEmpty()) {
                findings.add(ValidationIssue.semanticWarning(
                        "CONFIRMED_WITHOUT_SOURCE",
                        "Подтверждённый узел не имеет ссылки на источник",
                        nodeId));
            }
            for (JsonNode reference : references) {
                String sourceId = SemanticModel.text(reference, "sourceId");
                if (!sourceIds.contains(sourceId)) {
                    findings.add(ValidationIssue.semanticError(
                            "UNKNOWN_SOURCE_REFERENCE",
                            "Ссылка указывает на отсутствующий источник: "
                                    + sourceId,
                            nodeId));
                }
            }
        }
        return findings;
    }
}
