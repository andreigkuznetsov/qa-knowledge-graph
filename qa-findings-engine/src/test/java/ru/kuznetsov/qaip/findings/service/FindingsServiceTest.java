package ru.kuznetsov.qaip.findings.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;
import ru.kuznetsov.qagraph.validationcore.model.ValidationSummary;
import ru.kuznetsov.qaip.coverage.model.CoverageProblem;
import ru.kuznetsov.qaip.coverage.model.CoverageProblemType;
import ru.kuznetsov.qaip.coverage.model.CoverageReport;
import ru.kuznetsov.qaip.coverage.model.CoverageSeverity;
import ru.kuznetsov.qaip.coverage.service.CoverageService;
import ru.kuznetsov.qaip.findings.model.FindingCode;
import ru.kuznetsov.qaip.findings.model.FindingSeverity;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FindingsServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void fullyCoveredModelShouldReturnNoFindings() throws Exception {
        Fixture fixture = fixture(List.of(), true);

        var report = fixture.service().analyze(fixture.model());

        assertTrue(report.analyzed());
        assertTrue(report.findings().isEmpty());
        assertEquals(0, report.summary().total());
        verify(fixture.coverageService()).analyze(fixture.model());
    }

    @Test
    void shouldMapBusinessRuleGapToHighFinding() throws Exception {
        var report = fixture(List.of(problem(
                CoverageProblemType.MISSING_SCENARIO,
                "BR-001",
                "BUSINESS_RULE"
        )), true).analyze();

        var finding = report.findings().getFirst();
        assertEquals(FindingCode.BUSINESS_RULE_WITHOUT_SCENARIO, finding.code());
        assertEquals(FindingSeverity.HIGH, finding.severity());
        assertEquals("BR-001", finding.nodeId());
        assertEquals("BUSINESS_RULE", finding.nodeType());
        assertEquals(
                "Business rule BR-001 is not covered by any scenario.",
                finding.message()
        );
    }

    @Test
    void shouldMapScenarioGapToMediumFinding() throws Exception {
        var finding = fixture(List.of(problem(
                CoverageProblemType.MISSING_TEST_IMPLEMENTATION,
                "SC-001",
                "SCENARIO"
        )), true).analyze().findings().getFirst();

        assertEquals(FindingCode.SCENARIO_WITHOUT_TEST, finding.code());
        assertEquals(FindingSeverity.MEDIUM, finding.severity());
        assertEquals("SC-001", finding.nodeId());
    }

    @Test
    void shouldMapTestGapToMediumFinding() throws Exception {
        var finding = fixture(List.of(problem(
                CoverageProblemType.MISSING_CHECK,
                "TEST-001",
                "TEST_IMPLEMENTATION"
        )), true).analyze().findings().getFirst();

        assertEquals(FindingCode.TEST_WITHOUT_CHECK, finding.code());
        assertEquals(FindingSeverity.MEDIUM, finding.severity());
        assertEquals("TEST-001", finding.nodeId());
    }

    @Test
    void shouldReturnOneFindingPerProblemAndExactSummary() throws Exception {
        var report = fixture(List.of(
                problem(CoverageProblemType.MISSING_CHECK, "TEST-002", "TEST_IMPLEMENTATION"),
                problem(CoverageProblemType.MISSING_SCENARIO, "BR-002", "BUSINESS_RULE"),
                problem(CoverageProblemType.MISSING_TEST_IMPLEMENTATION, "SC-002", "SCENARIO"),
                problem(CoverageProblemType.MISSING_SCENARIO, "BR-001", "BUSINESS_RULE"),
                problem(CoverageProblemType.MISSING_CHECK, "TEST-001", "TEST_IMPLEMENTATION")
        ), true).analyze();

        assertEquals(5, report.summary().total());
        assertEquals(2, report.summary().high());
        assertEquals(3, report.summary().medium());
        assertEquals(0, report.summary().low());
        assertEquals(List.of(
                        "BUSINESS_RULE_WITHOUT_SCENARIO:BR-001",
                        "BUSINESS_RULE_WITHOUT_SCENARIO:BR-002",
                        "SCENARIO_WITHOUT_TEST:SC-002",
                        "TEST_WITHOUT_CHECK:TEST-001",
                        "TEST_WITHOUT_CHECK:TEST-002"
                ), report.findings().stream()
                        .map(finding -> finding.code() + ":" + finding.nodeId())
                        .toList());
    }

    @Test
    void repeatedAnalysisShouldBeEqualAndNotMutateInput() throws Exception {
        Fixture fixture = fixture(List.of(problem(
                CoverageProblemType.MISSING_CHECK,
                "TEST-001",
                "TEST_IMPLEMENTATION"
        )), true);
        JsonNode before = fixture.model().deepCopy();

        var first = fixture.service().analyze(fixture.model());
        var second = fixture.service().analyze(fixture.model());

        assertEquals(first, second);
        assertEquals(before, fixture.model());
    }

    @Test
    void invalidModelShouldNotProduceFindings() throws Exception {
        Fixture fixture = fixture(List.of(), false);

        var report = fixture.service().analyze(fixture.model());

        assertFalse(report.analyzed());
        assertTrue(report.findings().isEmpty());
        assertFalse(report.validation().valid());
    }

    @Test
    void emptyNodeCategoriesShouldProduceNoFindings() throws Exception {
        Fixture fixture = fixture(List.of(), true);

        var report = fixture.service().analyze(fixture.model());

        assertTrue(report.findings().isEmpty());
        assertEquals(0, report.summary().total());
    }

    private Fixture fixture(
            List<CoverageProblem> problems,
            boolean valid
    ) throws Exception {
        JsonNode model = objectMapper.readTree(
                "{\"nodes\":[],\"relationships\":[]}"
        );
        CoverageService coverageService = mock(CoverageService.class);
        when(coverageService.analyze(model))
                .thenReturn(coverageReport(problems, valid));
        return new Fixture(
                model,
                coverageService,
                new FindingsService(coverageService)
        );
    }

    private CoverageReport coverageReport(
            List<CoverageProblem> problems,
            boolean valid
    ) {
        QaModelValidationResult validation = new QaModelValidationResult(
                valid,
                "0.1",
                new ValidationSummary(valid ? 0 : 1, 0, valid ? 0 : 1),
                List.of()
        );
        return new CoverageReport(
                valid,
                "0.4",
                "0.1",
                Instant.EPOCH,
                null,
                List.of(),
                valid ? problems : List.of(),
                validation
        );
    }

    private CoverageProblem problem(
            CoverageProblemType type,
            String nodeId,
            String nodeType
    ) {
        return new CoverageProblem(
                type,
                CoverageSeverity.WARNING,
                nodeId,
                nodeType,
                nodeId,
                "coverage problem",
                "coverage explanation",
                "/nodes/0"
        );
    }

    private record Fixture(
            JsonNode model,
            CoverageService coverageService,
            FindingsService service
    ) {
        private ru.kuznetsov.qaip.findings.model.FindingsReport analyze() {
            return service.analyze(model);
        }
    }
}
