package ru.kuznetsov.qaip.findings.model;

import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;

import java.util.List;

public record FindingsReport(
        boolean analyzed,
        String schemaVersion,
        FindingsSummary summary,
        List<Finding> findings,
        QaModelValidationResult validation
) {
    public FindingsReport {
        findings = List.copyOf(findings);
    }
}
