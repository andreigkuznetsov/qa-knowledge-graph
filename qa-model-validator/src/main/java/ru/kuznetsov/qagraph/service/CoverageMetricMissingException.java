package ru.kuznetsov.qagraph.service;

import ru.kuznetsov.qaip.coverage.model.CoverageMetricCode;

public class CoverageMetricMissingException extends RuntimeException {

    public CoverageMetricMissingException(CoverageMetricCode metricCode) {
        super("Coverage metric is missing: " + metricCode.name());
    }
}
