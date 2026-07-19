package ru.kuznetsov.qaip.findings.service;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qaip.coverage.model.CoverageProblem;
import ru.kuznetsov.qaip.coverage.model.CoverageReport;
import ru.kuznetsov.qaip.coverage.service.CoverageService;
import ru.kuznetsov.qaip.findings.model.Finding;
import ru.kuznetsov.qaip.findings.model.FindingCode;
import ru.kuznetsov.qaip.findings.model.FindingSeverity;
import ru.kuznetsov.qaip.findings.model.FindingsReport;
import ru.kuznetsov.qaip.findings.model.FindingsSummary;

import java.util.Comparator;
import java.util.List;

public class FindingsService {

    private static final Comparator<Finding> FINDING_ORDER =
            Comparator.comparing(Finding::severity)
                    .thenComparing(Finding::code)
                    .thenComparing(Finding::nodeId);

    private final CoverageService coverageService;

    public FindingsService(CoverageService coverageService) {
        this.coverageService = coverageService;
    }

    public FindingsReport analyze(JsonNode model) {
        return analyze(coverageService.analyze(model));
    }

    public FindingsReport analyze(CoverageReport coverageReport) {

        if (!coverageReport.analyzed()) {
            return new FindingsReport(
                    false,
                    coverageReport.schemaVersion(),
                    emptySummary(),
                    List.of(),
                    coverageReport.validation()
            );
        }

        List<Finding> findings = coverageReport.problems().stream()
                .map(this::toFinding)
                .sorted(FINDING_ORDER)
                .toList();

        return new FindingsReport(
                true,
                coverageReport.schemaVersion(),
                summarize(findings),
                findings,
                coverageReport.validation()
        );
    }

    private Finding toFinding(CoverageProblem problem) {
        return switch (problem.type()) {
            case MISSING_SCENARIO -> new Finding(
                    FindingCode.BUSINESS_RULE_WITHOUT_SCENARIO,
                    FindingSeverity.HIGH,
                    problem.objectId(),
                    "BUSINESS_RULE",
                    "Business rule " + problem.objectId()
                            + " is not covered by any scenario.",
                    "Add at least one SCENARIO with a COVERS relationship "
                            + "to this BUSINESS_RULE."
            );
            case MISSING_TEST_IMPLEMENTATION -> new Finding(
                    FindingCode.SCENARIO_WITHOUT_TEST,
                    FindingSeverity.MEDIUM,
                    problem.objectId(),
                    "SCENARIO",
                    "Scenario " + problem.objectId()
                            + " has no validating test implementation.",
                    "Add at least one TEST_IMPLEMENTATION with a VALIDATES "
                            + "relationship to this SCENARIO."
            );
            case MISSING_CHECK -> new Finding(
                    FindingCode.TEST_WITHOUT_CHECK,
                    FindingSeverity.MEDIUM,
                    problem.objectId(),
                    "TEST_IMPLEMENTATION",
                    "Test implementation " + problem.objectId()
                            + " contains no checks.",
                    "Add at least one CHECK connected through a HAS_CHECK "
                            + "relationship."
            );
        };
    }

    private FindingsSummary summarize(List<Finding> findings) {
        int high = count(findings, FindingSeverity.HIGH);
        int medium = count(findings, FindingSeverity.MEDIUM);
        int low = count(findings, FindingSeverity.LOW);
        return new FindingsSummary(findings.size(), high, medium, low);
    }

    private int count(
            List<Finding> findings,
            FindingSeverity severity
    ) {
        return (int) findings.stream()
                .filter(finding -> finding.severity() == severity)
                .count();
    }

    private FindingsSummary emptySummary() {
        return new FindingsSummary(0, 0, 0, 0);
    }
}
