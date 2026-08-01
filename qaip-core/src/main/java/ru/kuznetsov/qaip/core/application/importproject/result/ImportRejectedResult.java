package ru.kuznetsov.qaip.core.application.importproject.result;

import java.util.List;
import java.util.Objects;

public record ImportRejectedResult(
        String stage,
        List<ImportFindingResult> findings
) implements ImportResult {
    public ImportRejectedResult {
        Objects.requireNonNull(stage, "stage");
        if (stage.isBlank()) throw new IllegalArgumentException("stage must not be blank");
        findings = List.copyOf(findings);
    }
}
