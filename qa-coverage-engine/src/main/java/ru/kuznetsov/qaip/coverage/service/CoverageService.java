package ru.kuznetsov.qaip.coverage.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import ru.kuznetsov.qagraph.validationcore.QaModelValidationEngine;
import ru.kuznetsov.qaip.coverage.analyzer.CheckCoverageAnalyzer;
import ru.kuznetsov.qaip.coverage.analyzer.RuleCoverageAnalyzer;
import ru.kuznetsov.qaip.coverage.analyzer.ScenarioCoverageAnalyzer;
import ru.kuznetsov.qaip.coverage.model.CoverageAnalysisResult;
import ru.kuznetsov.qaip.coverage.model.CoverageMetric;
import ru.kuznetsov.qaip.coverage.model.CoverageProblem;
import ru.kuznetsov.qaip.coverage.model.CoverageReport;
import ru.kuznetsov.qaip.coverage.model.CoverageSummary;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class CoverageService {

    private static final String RELEASE = "0.4";

    private final QaModelValidationEngine validationEngine;
    private final RuleCoverageAnalyzer ruleCoverageAnalyzer;
    private final ScenarioCoverageAnalyzer scenarioCoverageAnalyzer;
    private final CheckCoverageAnalyzer checkCoverageAnalyzer;

    public CoverageService(
            QaModelValidationEngine validationEngine,
            RuleCoverageAnalyzer ruleCoverageAnalyzer,
            ScenarioCoverageAnalyzer scenarioCoverageAnalyzer,
            CheckCoverageAnalyzer checkCoverageAnalyzer
    ) {
        this.validationEngine = validationEngine;
        this.ruleCoverageAnalyzer = ruleCoverageAnalyzer;
        this.scenarioCoverageAnalyzer =
                scenarioCoverageAnalyzer;
        this.checkCoverageAnalyzer = checkCoverageAnalyzer;
    }

    public CoverageReport analyze(JsonNode qaModel) {
        var validation = validationEngine.validate(qaModel);

        if (!validation.valid()) {
            return new CoverageReport(
                    false,
                    RELEASE,
                    validation.schemaVersion(),
                    Instant.now(),
                    null,
                    List.of(),
                    List.of(),
                    validation
            );
        }

        CoverageAnalysisResult ruleAnalysis =
                ruleCoverageAnalyzer.analyze(qaModel);

        CoverageAnalysisResult scenarioAnalysis =
                scenarioCoverageAnalyzer.analyze(qaModel);

        CoverageAnalysisResult checkAnalysis =
                checkCoverageAnalyzer.analyze(qaModel);

        List<CoverageMetric> metrics = new ArrayList<>();
        metrics.addAll(ruleAnalysis.metrics());
        metrics.addAll(scenarioAnalysis.metrics());
        metrics.addAll(checkAnalysis.metrics());

        List<CoverageProblem> problems =
                new ArrayList<>();
        problems.addAll(ruleAnalysis.problems());
        problems.addAll(scenarioAnalysis.problems());
        problems.addAll(checkAnalysis.problems());

        CoverageMetric ruleMetric = findMetric(
                metrics,
                RuleCoverageAnalyzer.METRIC_CODE
        );

        CoverageMetric scenarioMetric = findMetric(
                metrics,
                ScenarioCoverageAnalyzer.METRIC_CODE
        );

        CoverageMetric checkMetric = findMetric(
                metrics,
                CheckCoverageAnalyzer.METRIC_CODE
        );

        CoverageSummary summary = new CoverageSummary(
                ruleMetric.total(),
                ruleMetric.covered(),
                ruleMetric.uncovered(),
                ruleMetric.percentage(),
                scenarioMetric.total(),
                scenarioMetric.covered(),
                scenarioMetric.uncovered(),
                scenarioMetric.percentage(),
                checkMetric.total(),
                checkMetric.covered(),
                checkMetric.uncovered(),
                checkMetric.percentage(),
                problems.size()
        );

        return new CoverageReport(
                true,
                RELEASE,
                validation.schemaVersion(),
                Instant.now(),
                summary,
                List.copyOf(metrics),
                List.copyOf(problems),
                validation
        );
    }

    private CoverageMetric findMetric(
            List<CoverageMetric> metrics,
            String code
    ) {
        return metrics.stream()
                .filter(metric -> code.equals(metric.code()))
                .findFirst()
                .orElseThrow();
    }
}
