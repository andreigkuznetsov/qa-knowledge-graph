package ru.kuznetsov.qagraph.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import ru.kuznetsov.qagraph.api.CoverageResponseMapper;
import ru.kuznetsov.qagraph.api.RegisteredModelCoverageResponse;
import ru.kuznetsov.qagraph.repository.InMemoryQaModelRepository;
import ru.kuznetsov.qaip.coverage.model.CoverageReport;
import ru.kuznetsov.qaip.coverage.service.CoverageService;

@Service
public class RegisteredModelCoverageService {

    private final InMemoryQaModelRepository repository;
    private final CoverageService coverageService;
    private final CoverageResponseMapper responseMapper;

    public RegisteredModelCoverageService(
            InMemoryQaModelRepository repository,
            CoverageService coverageService,
            CoverageResponseMapper responseMapper
    ) {
        this.repository = repository;
        this.coverageService = coverageService;
        this.responseMapper = responseMapper;
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

        return responseMapper.map(modelId, report);
    }
}
