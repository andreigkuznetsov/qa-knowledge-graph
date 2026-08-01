package ru.kuznetsov.qaip.core.application.validation.rule;

import ru.kuznetsov.qaip.core.application.validation.ProjectValidationRule;
import ru.kuznetsov.qaip.core.application.validation.ValidationIssue;
import ru.kuznetsov.qaip.core.application.validation.ValidationSeverity;
import ru.kuznetsov.qaip.core.domain.Node;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.domain.Relationship;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public final class ScenarioWithoutTestValidationRule implements ProjectValidationRule {
    @Override
    public String ruleId() {
        return "SCENARIO_REQUIRES_TEST";
    }

    @Override
    public List<ValidationIssue> validate(Project project) {
        Objects.requireNonNull(project, "project");
        var testImplementationIds = new HashSet<String>();
        for (Node node : project.nodes()) {
            if ("TEST_IMPLEMENTATION".equals(node.type())) testImplementationIds.add(node.id());
        }
        var coveredScenarioIds = new HashSet<String>();
        for (Relationship relationship : project.relationships()) {
            if ("VALIDATES".equals(relationship.type())
                    && testImplementationIds.contains(relationship.from())) {
                coveredScenarioIds.add(relationship.to());
            }
        }
        return project.nodes().stream()
                .filter(node -> "SCENARIO".equals(node.type()))
                .filter(node -> !coveredScenarioIds.contains(node.id()))
                .map(node -> new ValidationIssue(
                        "SCENARIO_REQUIRES_TEST",
                        "SCENARIO_WITHOUT_TEST",
                        ValidationSeverity.ERROR,
                        "Scenario '" + node.id() + "' has no validating test implementation.",
                        node.id(),
                        null))
                .toList();
    }
}
