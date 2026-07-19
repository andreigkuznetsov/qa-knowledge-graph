package ru.kuznetsov.qagraph.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import ru.kuznetsov.qagraph.api.CoverageMetricResponse;
import ru.kuznetsov.qagraph.api.RegisteredModelCoverageResponse;
import ru.kuznetsov.qagraph.repository.InMemoryQaModelRepository;
import ru.kuznetsov.qaip.coverage.model.CoverageMetric;
import ru.kuznetsov.qaip.coverage.model.CoverageMetricCode;
import ru.kuznetsov.qaip.coverage.model.CoverageReport;
import ru.kuznetsov.qaip.coverage.service.CoverageService;

import java.util.Arrays;
import java.util.List;

@Service
public class RegisteredModelCoverageService {

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

        List<CoverageMetricResponse> metrics =
                Arrays.stream(CoverageMetricCode.values())
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
            CoverageMetricCode code
    ) {
        return report.metrics().stream()
                .filter(metric -> code.equals(metric.code()))
                .findFirst()
                .orElseThrow(() ->
                        new CoverageMetricMissingException(code));
    }

    private CoverageMetricResponse toResponse(CoverageMetric metric) {
        return new CoverageMetricResponse(
                metric.code().name(),
                metric.total(),
                metric.covered(),
                metric.uncovered(),
                metric.safePercentage()
        );
    }
}
