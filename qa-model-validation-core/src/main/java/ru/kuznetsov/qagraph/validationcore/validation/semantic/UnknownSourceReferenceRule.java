package ru.kuznetsov.qagraph.validationcore.validation.semantic;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class UnknownSourceReferenceRule implements KnowledgeRule {

    public static final String CODE = "UNKNOWN_SOURCE_REFERENCE";

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
            for (JsonNode reference : node.path("sourceReferences")) {
                String sourceId = SemanticModel.text(reference, "sourceId");
                if (!sourceIds.contains(sourceId)) {
                    findings.add(ValidationIssue.semanticError(
                            CODE,
                            "Ссылка указывает на отсутствующий источник: "
                                    + sourceId,
                            SemanticModel.text(node, "id")));
                }
            }
        }
        return findings;
    }
}
