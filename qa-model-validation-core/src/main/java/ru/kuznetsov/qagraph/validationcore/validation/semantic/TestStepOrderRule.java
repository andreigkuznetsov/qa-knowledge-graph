package ru.kuznetsov.qagraph.validationcore.validation.semantic;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TestStepOrderRule implements KnowledgeRule {

    public static final String CODE = "DUPLICATE_TEST_STEP_ORDER";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<ValidationIssue> evaluate(JsonNode model) {
        List<ValidationIssue> findings = new ArrayList<>();
        for (JsonNode node : model.path("nodes")) {
            if (!"TEST_IMPLEMENTATION".equals(
                    SemanticModel.text(node, "type"))) {
                continue;
            }
            Set<Integer> orders = new HashSet<>();
            for (JsonNode step : node.path("testImplementation").path("steps")) {
                int order = step.path("order").asInt();
                if (!orders.add(order)) {
                    findings.add(ValidationIssue.semanticError(
                            CODE,
                            "В тесте повторяется номер шага: " + order,
                            SemanticModel.text(node, "id")));
                }
            }
        }
        return findings;
    }
}
