package ru.kuznetsov.qagraph.api;

import org.springframework.stereotype.Component;
import ru.kuznetsov.qagraph.service.CoverageMetricMissingException;
import ru.kuznetsov.qaip.coverage.model.CoverageMetric;
import ru.kuznetsov.qaip.coverage.model.CoverageMetricCode;
import ru.kuznetsov.qaip.coverage.model.CoverageReport;

import java.util.Arrays;
import java.util.List;

@Component
public class CoverageResponseMapper {

    public RegisteredModelCoverageResponse map(
            String modelId,
            CoverageReport report
    ) {
        List<CoverageMetricResponse> metrics =
                Arrays.stream(CoverageMetricCode.values())
                        .map(code -> findMetric(report, code))
                        .map(this::map)
                        .toList();

        return new RegisteredModelCoverageResponse(
                modelId,
                report.analyzed(),
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
                .filter(metric -> code == metric.code())
                .findFirst()
                .orElseThrow(() ->
                        new CoverageMetricMissingException(code));
    }

    private CoverageMetricResponse map(CoverageMetric metric) {
        return new CoverageMetricResponse(
                metric.code().name(),
                metric.total(),
                metric.covered(),
                metric.uncovered(),
                metric.safePercentage()
        );
    }
}
