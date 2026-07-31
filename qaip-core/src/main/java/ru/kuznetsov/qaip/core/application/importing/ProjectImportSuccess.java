package ru.kuznetsov.qaip.core.application.importing;

import ru.kuznetsov.qaip.core.validation.ApplicationValidProjectDocument;

import java.util.List;
import java.util.Objects;

public record ProjectImportSuccess(ApplicationValidProjectDocument document,
                                   List<ProjectImportFinding> warnings) implements ProjectImportResult {
    public ProjectImportSuccess {
        Objects.requireNonNull(document, "document");
        warnings = List.copyOf(warnings);
        if (warnings.stream().anyMatch(finding -> finding.severity() != ProjectImportSeverity.WARNING)) {
            throw new IllegalArgumentException("success may contain warning findings only");
        }
    }
}
