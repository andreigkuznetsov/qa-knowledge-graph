package ru.kuznetsov.qaip.core.validation;

import java.util.List;
import java.util.Objects;

public record ApplicationValidationSuccess(ApplicationValidProjectDocument document,
                                           List<ApplicationValidationFinding> warnings)
        implements ApplicationValidationResult {
    public ApplicationValidationSuccess {
        Objects.requireNonNull(document, "document");
        warnings = List.copyOf(warnings);
        if (warnings.stream().anyMatch(finding -> finding.severity() != ApplicationValidationSeverity.WARNING)) {
            throw new IllegalArgumentException("success may contain warning findings only");
        }
    }
}
