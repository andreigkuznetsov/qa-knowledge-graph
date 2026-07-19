package ru.kuznetsov.qagraph.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import ru.kuznetsov.qagraph.api.CoverageMetricResponse;
import ru.kuznetsov.qagraph.api.RegisteredModelCoverageResponse;
import ru.kuznetsov.qagraph.repository.InMemoryQaModelRepository;
import ru.kuznetsov.qaip.coverage.model.CoverageMetric;
import ru.kuznetsov.qaip.coverage.model.CoverageReport;
import ru.kuznetsov.qaip.coverage.service.CoverageService;

import java.util.List;

@Service
public class RegisteredModelCoverageService {

    private static final List<String> METRIC_ORDER = List.of(
            "RULE_SCENARIO_COVERAGE",
            "SCENARIO_TEST_COVERAGE",
            "TEST_CHECK_COVERAGE"
    );

    private final InMemoryQaModelRepository repository;
    private final CoverageService coverageService;

    public RegisteredModelCoverageService(
            InMemoryQaModelRepository repository,
            CoverageService coverageService
    ) {
        this.repository = repository;
        this.coverageService = coverageService;
    }

    public RegisteredModelCoverageResponse analyze(String modelId) {
        JsonNode model = repository.findById(modelId)
                .orElseThrow(() ->
                        new QaModelNotFoundException(modelId)
                );
        CoverageReport report = coverageService.analyze(model);

        if (!report.analyzed()) {
            throw new InvalidQaModelException(report.validation());
        }

        List<CoverageMetricResponse> metrics = METRIC_ORDER.stream()
                .map(code -> findMetric(report, code))
                .map(this::toResponse)
                .toList();

        return new RegisteredModelCoverageResponse(
                modelId,
                true,
                report.schemaVersion(),
                metrics,
                report.problems(),
                report.validation()
        );
    }

    private CoverageMetric findMetric(
            CoverageReport report,
            String code
    ) {
        return report.metrics().stream()
                .filter(metric -> code.equals(metric.code()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Coverage metric is missing: " + code
                ));
    }

    private CoverageMetricResponse toResponse(CoverageMetric metric) {
        double percentage = metric.total() == 0
                ? 0.0
                : metric.percentage();

        return new CoverageMetricResponse(
                metric.code(),
                metric.total(),
                metric.covered(),
                metric.uncovered(),
                percentage
        );
    }
}
