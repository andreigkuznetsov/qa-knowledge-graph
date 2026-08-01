package ru.kuznetsov.qaip.core.application.validation;

import ru.kuznetsov.qaip.core.domain.Project;

import java.util.List;

public interface ProjectValidationRule {
    String ruleId();

    List<ValidationIssue> validate(Project project);
}
