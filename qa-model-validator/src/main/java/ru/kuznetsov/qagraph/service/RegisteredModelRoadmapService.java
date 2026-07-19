package ru.kuznetsov.qagraph.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import ru.kuznetsov.qagraph.api.RegisteredModelRoadmapResponse;
import ru.kuznetsov.qagraph.api.RoadmapResponseMapper;
import ru.kuznetsov.qagraph.repository.InMemoryQaModelRepository;
import ru.kuznetsov.qaip.coverage.model.CoverageReport;
import ru.kuznetsov.qaip.coverage.service.CoverageService;
import ru.kuznetsov.qaip.findings.model.FindingsReport;
import ru.kuznetsov.qaip.findings.service.FindingsService;
import ru.kuznetsov.qaip.roadmap.model.RoadmapReport;
import ru.kuznetsov.qaip.roadmap.service.RoadmapService;

@Service
public class RegisteredModelRoadmapService {

    private final InMemoryQaModelRepository repository;
    private final CoverageService coverageService;
    private final FindingsService findingsService;
    private final RoadmapService roadmapService;
    private final RoadmapResponseMapper responseMapper;

    public RegisteredModelRoadmapService(
            InMemoryQaModelRepository repository,
            CoverageService coverageService,
            FindingsService findingsService,
            RoadmapService roadmapService,
            RoadmapResponseMapper responseMapper
    ) {
        this.repository = repository;
        this.coverageService = coverageService;
        this.findingsService = findingsService;
        this.roadmapService = roadmapService;
        this.responseMapper = responseMapper;
    }

    public RegisteredModelRoadmapResponse plan(String modelId) {
        JsonNode model = repository.findById(modelId)
                .orElseThrow(() -> new QaModelNotFoundException(modelId));
        CoverageReport coverageReport = coverageService.analyze(model);

        if (!coverageReport.analyzed()) {
            throw new InvalidQaModelException(coverageReport.validation());
        }

        FindingsReport findingsReport =
                findingsService.analyze(coverageReport);
        RoadmapReport roadmapReport = roadmapService.plan(findingsReport);

        return responseMapper.map(
                modelId,
                roadmapReport,
                coverageReport.validation()
        );
    }
}
