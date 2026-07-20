package ru.kuznetsov.qagraph.validationcore.validation.semantic;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;

import java.util.ArrayList;
import java.util.List;

public final class ConfirmedWithoutSourceRule implements KnowledgeRule {

    public static final String CODE = "CONFIRMED_WITHOUT_SOURCE";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<ValidationIssue> evaluate(JsonNode model) {
        List<ValidationIssue> findings = new ArrayList<>();
        for (JsonNode node : model.path("nodes")) {
            if ("CONFIRMED".equals(SemanticModel.text(node, "status"))
                    && node.path("sourceReferences").isEmpty()) {
                findings.add(ValidationIssue.semanticWarning(
                        CODE,
                        "Подтверждённый узел не имеет ссылки на источник",
                        SemanticModel.text(node, "id")));
            }
        }
        return findings;
    }
}
