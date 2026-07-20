package ru.kuznetsov.qagraph.validationcore.validation.semantic;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SemanticValidationEngine {

    private final List<KnowledgeRule> rules;

    public SemanticValidationEngine(List<? extends KnowledgeRule> rules) {
        Objects.requireNonNull(rules, "rules must not be null");
        this.rules = List.copyOf(rules);
    }

    public List<ValidationIssue> validate(JsonNode model) {
        Objects.requireNonNull(model, "model must not be null");
        List<ValidationIssue> findings = new ArrayList<>();
        for (KnowledgeRule rule : rules) {
            findings.addAll(rule.evaluate(model));
        }
        return List.copyOf(findings);
    }
}
