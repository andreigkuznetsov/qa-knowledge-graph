package ru.kuznetsov.qagraph.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import ru.kuznetsov.qagraph.api.AssessmentCoverageSummaryResponse;
import ru.kuznetsov.qagraph.api.AssessmentHealth;
import ru.kuznetsov.qagraph.api.AssessmentSummaryResponse;
import ru.kuznetsov.qagraph.api.AssessmentValidationSummaryResponse;
import ru.kuznetsov.qagraph.api.CoverageResponseMapper;
import ru.kuznetsov.qagraph.api.FindingsResponseMapper;
import ru.kuznetsov.qagraph.api.RegisteredModelAssessmentResponse;
import ru.kuznetsov.qagraph.api.RegisteredModelCoverageResponse;
import ru.kuznetsov.qagraph.api.RegisteredModelFindingsResponse;
import ru.kuznetsov.qagraph.repository.InMemoryQaModelRepository;
import ru.kuznetsov.qaip.coverage.model.CoverageReport;
import ru.kuznetsov.qaip.coverage.service.CoverageService;
import ru.kuznetsov.qaip.findings.model.FindingsReport;
import ru.kuznetsov.qaip.findings.service.FindingsService;

@Service
public class RegisteredModelAssessmentService {

    private final InMemoryQaModelRepository repository;
    private final CoverageService coverageService;
    private final FindingsService findingsService;
    private final CoverageResponseMapper coverageMapper;
    private final FindingsResponseMapper findingsMapper;

    public RegisteredModelAssessmentService(
            InMemoryQaModelRepository repository,
            CoverageService coverageService,
            FindingsService findingsService,
            CoverageResponseMapper coverageMapper,
            FindingsResponseMapper findingsMapper
    ) {
        this.repository = repository;
        this.coverageService = coverageService;
        this.findingsService = findingsService;
        this.coverageMapper = coverageMapper;
        this.findingsMapper = findingsMapper;
    }

    public RegisteredModelAssessmentResponse assess(String modelId) {
        JsonNode model = repository.findById(modelId)
                .orElseThrow(() -> new QaModelNotFoundException(modelId));
        CoverageReport coverageReport = coverageService.analyze(model);

        if (!coverageReport.analyzed()) {
            throw new InvalidQaModelException(coverageReport.validation());
        }

        FindingsReport findingsReport =
                findingsService.analyze(coverageReport);
        RegisteredModelCoverageResponse coverage =
                coverageMapper.map(modelId, coverageReport);
        RegisteredModelFindingsResponse findings =
                findingsMapper.map(modelId, findingsReport);

        return new RegisteredModelAssessmentResponse(
                modelId,
                true,
                coverageReport.schemaVersion(),
                AssessmentHealth.from(
                        coverageReport.validation().valid(),
                        findings.summary().high(),
                        findings.summary().medium()
                ),
                summary(coverage, findings),
                coverage,
                findings,
                coverageReport.validation()
        );
    }

    private AssessmentSummaryResponse summary(
            RegisteredModelCoverageResponse coverage,
            RegisteredModelFindingsResponse findings
    ) {
        var validation = coverage.validation();
        return new AssessmentSummaryResponse(
                new AssessmentValidationSummaryResponse(
                        validation.valid(),
                        validation.summary().errors(),
                        validation.summary().warnings()
                ),
                new AssessmentCoverageSummaryResponse(
                        coverage.metrics().get(0).coveragePercent(),
                        coverage.metrics().get(1).coveragePercent(),
                        coverage.metrics().get(2).coveragePercent()
                ),
                findings.summary()
        );
    }
}
