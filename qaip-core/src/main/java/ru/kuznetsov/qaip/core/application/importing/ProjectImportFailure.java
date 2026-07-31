package ru.kuznetsov.qaip.core.application.importing;

import java.util.List;
import java.util.Objects;

public record ProjectImportFailure(ProjectImportStage failedStage,
                                   List<ProjectImportFinding> findings) implements ProjectImportResult {
    public ProjectImportFailure {
        Objects.requireNonNull(failedStage, "failedStage");
        findings = List.copyOf(findings);
        if (findings.isEmpty()) throw new IllegalArgumentException("findings must not be empty");
        if (findings.stream().anyMatch(finding -> finding.stage() != failedStage)) {
            throw new IllegalArgumentException("every finding must belong to the failed stage");
        }
        if (findings.stream().noneMatch(finding -> finding.severity() == ProjectImportSeverity.ERROR)) {
            throw new IllegalArgumentException("failure must contain an error finding");
        }
    }
}
