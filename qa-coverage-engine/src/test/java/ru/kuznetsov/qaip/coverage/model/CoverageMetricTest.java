package ru.kuznetsov.qaip.coverage.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CoverageMetricTest {

    @Test
    void shouldReturnZeroSafePercentageForEmptyMetric() {
        CoverageMetric metric = new CoverageMetric(
                CoverageMetricCode.RULE_SCENARIO_COVERAGE,
                "rules",
                0,
                0,
                0,
                100.0
        );

        assertEquals(0.0, metric.safePercentage());
    }

    @Test
    void shouldPreserveCalculatedPercentageForNonEmptyMetric() {
        CoverageMetric metric = new CoverageMetric(
                CoverageMetricCode.SCENARIO_TEST_COVERAGE,
                "scenarios",
                3,
                2,
                1,
                66.67
        );

        assertEquals(66.67, metric.safePercentage());
    }

    @Test
    void shouldDefineStableMetricOrder() {
        assertArrayEquals(
                new CoverageMetricCode[]{
                        CoverageMetricCode.RULE_SCENARIO_COVERAGE,
                        CoverageMetricCode.SCENARIO_TEST_COVERAGE,
                        CoverageMetricCode.TEST_CHECK_COVERAGE
                },
                CoverageMetricCode.values()
        );
    }
}
