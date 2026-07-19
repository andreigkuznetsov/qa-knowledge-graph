package ru.kuznetsov.qagraph.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.repository.InMemoryQaModelRepository;
import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;
import ru.kuznetsov.qagraph.validationcore.model.ValidationSummary;
import ru.kuznetsov.qaip.coverage.model.CoverageMetric;
import ru.kuznetsov.qaip.coverage.model.CoverageReport;
import ru.kuznetsov.qaip.coverage.service.CoverageService;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegisteredModelCoverageServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldLoadModelDelegateAnalysisAndMapStableMetricOrder()
            throws Exception {
        InMemoryQaModelRepository repository =
                new InMemoryQaModelRepository();
        CoverageService coverageService = mock(CoverageService.class);
        JsonNode model = objectMapper.readTree(
                "{\"nodes\":[],\"relationships\":[]}"
        );
        String modelId = repository.save(model, 0).modelId();
        when(coverageService.analyze(any())).thenReturn(report(
                new CoverageMetric("TEST_CHECK_COVERAGE", "checks", 4, 3, 1, 75.0),
                new CoverageMetric("RULE_SCENARIO_COVERAGE", "rules", 2, 1, 1, 50.0),
                new CoverageMetric("SCENARIO_TEST_COVERAGE", "scenarios", 3, 2, 1, 66.67)
        ));
        RegisteredModelCoverageService service =
                new RegisteredModelCoverageService(repository, coverageService);

        var response = service.analyze(modelId);

        assertEquals(modelId, response.modelId());
        assertEquals(List.of(
                        "RULE_SCENARIO_COVERAGE",
                        "SCENARIO_TEST_COVERAGE",
                        "TEST_CHECK_COVERAGE"
                ), response.metrics().stream()
                        .map(metric -> metric.metric())
                        .toList());
        assertEquals(2, response.metrics().get(0).total());
        assertEquals(1, response.metrics().get(0).covered());
        assertEquals(1, response.metrics().get(0).uncovered());
        assertEquals(50.0, response.metrics().get(0).coveragePercent());
        verify(coverageService).analyze(any());
    }

    @Test
    void shouldNormalizeEmptyCategoryPercentageAndRemainRepeatable()
            throws Exception {
        InMemoryQaModelRepository repository =
                new InMemoryQaModelRepository();
        CoverageService coverageService = mock(CoverageService.class);
        JsonNode model = objectMapper.readTree(
                "{\"nodes\":[],\"relationships\":[]}"
        );
        String modelId = repository.save(model, 0).modelId();
        when(coverageService.analyze(any())).thenReturn(report(
                new CoverageMetric("RULE_SCENARIO_COVERAGE", "rules", 0, 0, 0, 100.0),
                new CoverageMetric("SCENARIO_TEST_COVERAGE", "scenarios", 0, 0, 0, 100.0),
                new CoverageMetric("TEST_CHECK_COVERAGE", "checks", 0, 0, 0, 100.0)
        ));
        RegisteredModelCoverageService service =
                new RegisteredModelCoverageService(repository, coverageService);

        var first = service.analyze(modelId);
        var second = service.analyze(modelId);

        assertEquals(first, second);
        first.metrics().forEach(metric ->
                assertEquals(0.0, metric.coveragePercent()));
        assertEquals(model, repository.findById(modelId).orElseThrow());
        verify(coverageService, times(2)).analyze(any());
    }

    @Test
    void shouldUseExistingNotFoundException() {
        RegisteredModelCoverageService service =
                new RegisteredModelCoverageService(
                        new InMemoryQaModelRepository(),
                        mock(CoverageService.class)
                );

        assertThrows(
                QaModelNotFoundException.class,
                () -> service.analyze("unknown")
        );
    }

    private CoverageReport report(CoverageMetric... metrics) {
        QaModelValidationResult validation =
                new QaModelValidationResult(
                        true,
                        "0.1",
                        new ValidationSummary(0, 0, 0),
                        List.of()
                );
        return new CoverageReport(
                true,
                "0.4",
                "0.1",
                Instant.EPOCH,
                null,
                List.of(metrics),
                List.of(),
                validation
        );
    }
}
