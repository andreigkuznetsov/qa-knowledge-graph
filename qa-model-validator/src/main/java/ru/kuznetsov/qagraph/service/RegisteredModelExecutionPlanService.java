package ru.kuznetsov.qagraph.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import ru.kuznetsov.qagraph.api.ExecutionPlanResponseMapper;
import ru.kuznetsov.qagraph.api.RegisteredModelExecutionPlanResponse;
import ru.kuznetsov.qagraph.repository.InMemoryQaModelRepository;
import ru.kuznetsov.qaip.coverage.model.CoverageReport;
import ru.kuznetsov.qaip.coverage.service.CoverageService;
import ru.kuznetsov.qaip.execution.model.ExecutionPlan;
import ru.kuznetsov.qaip.execution.service.ExecutionPlanner;
import ru.kuznetsov.qaip.findings.model.FindingsReport;
import ru.kuznetsov.qaip.findings.service.FindingsService;
import ru.kuznetsov.qaip.roadmap.model.RoadmapReport;
import ru.kuznetsov.qaip.roadmap.service.RoadmapService;

@Service
public class RegisteredModelExecutionPlanService {

    private final InMemoryQaModelRepository repository;
    private final CoverageService coverageService;
    private final FindingsService findingsService;
    private final RoadmapService roadmapService;
    private final ExecutionPlanner executionPlanner;
    private final ExecutionPlanResponseMapper responseMapper;

    public RegisteredModelExecutionPlanService(
            InMemoryQaModelRepository repository,
            CoverageService coverageService,
            FindingsService findingsService,
            RoadmapService roadmapService,
            ExecutionPlanner executionPlanner,
            ExecutionPlanResponseMapper responseMapper
    ) {
        this.repository = repository;
        this.coverageService = coverageService;
        this.findingsService = findingsService;
        this.roadmapService = roadmapService;
        this.executionPlanner = executionPlanner;
        this.responseMapper = responseMapper;
    }

    public RegisteredModelExecutionPlanResponse plan(String modelId) {
        JsonNode model = repository.findById(modelId)
                .orElseThrow(() -> new QaModelNotFoundException(modelId));
        CoverageReport coverageReport = coverageService.analyze(model);

        if (!coverageReport.analyzed()) {
            throw new InvalidQaModelException(coverageReport.validation());
        }

        FindingsReport findingsReport = findingsService.analyze(coverageReport);
        RoadmapReport roadmapReport = roadmapService.plan(findingsReport);
        ExecutionPlan executionPlan = executionPlanner.plan(roadmapReport);

        return responseMapper.map(
                modelId,
                executionPlan,
                coverageReport.validation()
        );
    }
}
