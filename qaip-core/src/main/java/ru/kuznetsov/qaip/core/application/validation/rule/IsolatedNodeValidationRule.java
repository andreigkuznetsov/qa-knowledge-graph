package ru.kuznetsov.qaip.core.application.validation.rule;

import ru.kuznetsov.qaip.core.application.validation.ProjectValidationRule;
import ru.kuznetsov.qaip.core.application.validation.ValidationIssue;
import ru.kuznetsov.qaip.core.application.validation.ValidationSeverity;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.domain.Relationship;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public final class IsolatedNodeValidationRule implements ProjectValidationRule {
    @Override
    public String ruleId() {
        return "ISOLATED_NODE";
    }

    @Override
    public List<ValidationIssue> validate(Project project) {
        Objects.requireNonNull(project, "project");
        var connectedNodeIds = new HashSet<String>();
        for (Relationship relationship : project.relationships()) {
            connectedNodeIds.add(relationship.from());
            connectedNodeIds.add(relationship.to());
        }
        return project.nodes().stream()
                .filter(node -> !connectedNodeIds.contains(node.id()))
                .map(node -> new ValidationIssue(
                        "ISOLATED_NODE",
                        "NODE_WITHOUT_RELATIONSHIPS",
                        ValidationSeverity.WARNING,
                        "Node '" + node.id() + "' has no relationships.",
                        node.id(),
                        null))
                .toList();
    }
}
