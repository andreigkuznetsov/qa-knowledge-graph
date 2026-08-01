package ru.kuznetsov.qaip.core.application.validation;

import ru.kuznetsov.qaip.core.domain.Project;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public final class ValidationEngine {
    private final List<ProjectValidationRule> rules;

    public ValidationEngine(List<ProjectValidationRule> rules) {
        this.rules = List.copyOf(rules);
        var ruleIds = new HashSet<String>();
        for (ProjectValidationRule rule : this.rules) {
            String ruleId = Objects.requireNonNull(rule.ruleId(), "ruleId");
            if (ruleId.isBlank()) throw new IllegalArgumentException("ruleId must not be blank");
            if (!ruleIds.add(ruleId)) throw new IllegalArgumentException("duplicate ruleId: " + ruleId);
        }
    }

    public ValidationReport validate(Project project) {
        Objects.requireNonNull(project, "project");
        var issues = new ArrayList<ValidationIssue>();
        for (ProjectValidationRule rule : rules) {
            String executingRuleId = rule.ruleId();
            List<ValidationIssue> ruleIssues = List.copyOf(
                    Objects.requireNonNull(rule.validate(project), "rule result for " + executingRuleId));
            for (ValidationIssue issue : ruleIssues) {
                if (!executingRuleId.equals(issue.ruleId())) {
                    throw new IllegalStateException("rule " + executingRuleId
                            + " returned issue for incompatible rule " + issue.ruleId());
                }
            }
            issues.addAll(ruleIssues);
        }
        return new ValidationReport(issues);
    }
}
