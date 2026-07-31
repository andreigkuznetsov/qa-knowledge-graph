package ru.kuznetsov.qaip.core.validation;

import java.util.List;

public record ApplicationValidationFailure(List<ApplicationValidationFinding> findings)
        implements ApplicationValidationResult {
    public ApplicationValidationFailure {
        findings = List.copyOf(findings);
        if (findings.isEmpty()) throw new IllegalArgumentException("findings must not be empty");
        if (findings.stream().noneMatch(finding -> finding.severity() == ApplicationValidationSeverity.ERROR)) {
            throw new IllegalArgumentException("failure must contain an error finding");
        }
    }
}
