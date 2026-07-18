package ru.kuznetsov.qaip.analysis.adapter.coverage;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qagraph.validationcore.QaModelValidationEngine;
import ru.kuznetsov.qaip.core.analysis.AnalysisContext;
import ru.kuznetsov.qaip.core.engine.AbstractAnalysisEngineAdapter;
import ru.kuznetsov.qaip.coverage.analyzer.CheckCoverageAnalyzer;
import ru.kuznetsov.qaip.coverage.analyzer.RuleCoverageAnalyzer;
import ru.kuznetsov.qaip.coverage.analyzer.ScenarioCoverageAnalyzer;
import ru.kuznetsov.qaip.coverage.model.CoverageReport;
import ru.kuznetsov.qaip.coverage.service.CoverageService;

import java.util.Objects;

public final class CoverageAnalysisEngine
        extends AbstractAnalysisEngineAdapter<CoverageReport> {

    private static final int ORDER = 200;

    private final CoverageService coverageService;

    public CoverageAnalysisEngine() {
        this(
                new CoverageService(
                        new QaModelValidationEngine(),
                        new RuleCoverageAnalyzer(),
                        new ScenarioCoverageAnalyzer(),
                        new CheckCoverageAnalyzer()
                ),
                new CoverageAssessmentMapper()
        );
    }

    CoverageAnalysisEngine(
            CoverageService coverageService,
            CoverageAssessmentMapper mapper
    ) {
        super(mapper);
        this.coverageService = Objects.requireNonNull(
                coverageService,
                "coverageService must not be null"
        );
    }

    @Override
    public String id() {
        return CoverageAssessmentMapper.ENGINE_ID;
    }

    @Override
    public String name() {
        return CoverageAssessmentMapper.ENGINE_NAME;
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    protected CoverageReport execute(
            JsonNode qaModel,
            AnalysisContext context
    ) {
        return coverageService.analyze(qaModel);
    }
}
