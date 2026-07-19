package ru.kuznetsov.qagraph.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.api.AssessmentHealth;
import ru.kuznetsov.qagraph.api.CoverageResponseMapper;
import ru.kuznetsov.qagraph.api.FindingsResponseMapper;
import ru.kuznetsov.qagraph.repository.InMemoryQaModelRepository;
import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;
import ru.kuznetsov.qagraph.validationcore.model.ValidationSummary;
import ru.kuznetsov.qaip.coverage.model.CoverageMetric;
import ru.kuznetsov.qaip.coverage.model.CoverageMetricCode;
import ru.kuznetsov.qaip.coverage.model.CoverageReport;
import ru.kuznetsov.qaip.coverage.service.CoverageService;
import ru.kuznetsov.qaip.findings.model.Finding;
import ru.kuznetsov.qaip.findings.model.FindingCode;
import ru.kuznetsov.qaip.findings.model.FindingSeverity;
import ru.kuznetsov.qaip.findings.model.FindingsReport;
import ru.kuznetsov.qaip.findings.model.FindingsSummary;
import ru.kuznetsov.qaip.findings.service.FindingsService;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegisteredModelAssessmentServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldOrchestrateExistingReportsOnceAndMapWarningSummary()
            throws Exception {
        Fixture fixture = fixture(true, true);

        var response = fixture.service().assess(fixture.modelId());

        assertEquals(fixture.modelId(), response.modelId());
        assertEquals(AssessmentHealth.WARNING, response.health());
        assertEquals(true, response.summary().validation().valid());
        assertEquals(2, response.summary().validation().warnings());
        assertEquals(100.0, response.summary().coverage().ruleScenarioCoverage());
        assertEquals(50.0, response.summary().coverage().scenarioTestCoverage());
        assertEquals(75.0, response.summary().coverage().testCheckCoverage());
        assertEquals(1, response.summary().findings().total());
        assertEquals(response.validation(), response.coverage().validation());
        verify(fixture.coverageService()).analyze(any());
        verify(fixture.findingsService()).analyze(fixture.coverageReport());
    }

    @Test
    void shouldReturnEqualAssessmentForRepeatedCalls() throws Exception {
        Fixture fixture = fixture(true, false);

        var first = fixture.service().assess(fixture.modelId());
        var second = fixture.service().assess(fixture.modelId());

        assertEquals(first, second);
        assertEquals(AssessmentHealth.PASS, first.health());
        verify(fixture.coverageService(), times(2)).analyze(any());
        verify(fixture.findingsService(), times(2))
                .analyze(fixture.coverageReport());
    }

    @Test
    void unknownModelShouldUseExistingException() {
        RegisteredModelAssessmentService service = service(
                new InMemoryQaModelRepository(),
                mock(CoverageService.class),
                mock(FindingsService.class)
        );

        assertThrows(
                QaModelNotFoundException.class,
                () -> service.assess("unknown")
        );
    }

    @Test
    void invalidStoredModelShouldUseExistingValidationException()
            throws Exception {
        InMemoryQaModelRepository repository = new InMemoryQaModelRepository();
        String modelId = repository.save(objectMapper.readTree("{}"), 0)
                .modelId();
        CoverageService coverageService = mock(CoverageService.class);
        FindingsService findingsService = mock(FindingsService.class);
        CoverageReport report = coverageReport(false);
        when(coverageService.analyze(any())).thenReturn(report);
        RegisteredModelAssessmentService service = service(
                repository,
                coverageService,
                findingsService
        );

        InvalidQaModelException exception = assertThrows(
                InvalidQaModelException.class,
                () -> service.assess(modelId)
        );

        assertEquals(report.validation(), exception.validationResult());
        verify(findingsService, times(0)).analyze(report);
    }

    private Fixture fixture(boolean valid, boolean withFinding)
            throws Exception {
        InMemoryQaModelRepository repository = new InMemoryQaModelRepository();
        JsonNode model = objectMapper.readTree(
                "{\"nodes\":[],\"relationships\":[]}"
        );
        String modelId = repository.save(model, 0).modelId();
        CoverageService coverageService = mock(CoverageService.class);
        FindingsService findingsService = mock(FindingsService.class);
        CoverageReport coverageReport = coverageReport(valid);
        FindingsReport findingsReport = findingsReport(withFinding);
        when(coverageService.analyze(any())).thenReturn(coverageReport);
        when(findingsService.analyze(coverageReport))
                .thenReturn(findingsReport);
        return new Fixture(
                modelId,
                coverageReport,
                coverageService,
                findingsService,
                service(repository, coverageService, findingsService)
        );
    }

    private RegisteredModelAssessmentService service(
            InMemoryQaModelRepository repository,
            CoverageService coverageService,
            FindingsService findingsService
    ) {
        return new RegisteredModelAssessmentService(
                repository,
                coverageService,
                findingsService,
                new CoverageResponseMapper(),
                new FindingsResponseMapper()
        );
    }

    private CoverageReport coverageReport(boolean valid) {
        QaModelValidationResult validation = new QaModelValidationResult(
                valid,
                valid ? "0.1" : null,
                new ValidationSummary(valid ? 0 : 1, valid ? 2 : 0, valid ? 2 : 1),
                List.of()
        );
        return new CoverageReport(
                valid,
                "0.4",
                valid ? "0.1" : null,
                Instant.EPOCH,
                null,
                valid ? List.of(
                        metric(CoverageMetricCode.RULE_SCENARIO_COVERAGE, 100.0),
                        metric(CoverageMetricCode.SCENARIO_TEST_COVERAGE, 50.0),
                        metric(CoverageMetricCode.TEST_CHECK_COVERAGE, 75.0)
                ) : List.of(),
                List.of(),
                validation
        );
    }

    private CoverageMetric metric(CoverageMetricCode code, double percentage) {
        return new CoverageMetric(code, code.name(), 4, 3, 1, percentage);
    }

    private FindingsReport findingsReport(boolean withFinding) {
        List<Finding> findings = withFinding ? List.of(new Finding(
                FindingCode.SCENARIO_WITHOUT_TEST,
                FindingSeverity.MEDIUM,
                "SC-002",
                "SCENARIO",
                "message",
                "recommendation"
        )) : List.of();
        return new FindingsReport(
                true,
                "0.1",
                new FindingsSummary(findings.size(), 0, findings.size(), 0),
                findings,
                coverageReport(true).validation()
        );
    }

    private record Fixture(
            String modelId,
            CoverageReport coverageReport,
            CoverageService coverageService,
            FindingsService findingsService,
            RegisteredModelAssessmentService service
    ) {
    }
}
