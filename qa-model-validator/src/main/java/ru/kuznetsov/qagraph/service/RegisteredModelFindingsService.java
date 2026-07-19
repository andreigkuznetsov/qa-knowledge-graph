package ru.kuznetsov.qagraph.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import ru.kuznetsov.qagraph.api.FindingsResponseMapper;
import ru.kuznetsov.qagraph.api.RegisteredModelFindingsResponse;
import ru.kuznetsov.qagraph.repository.InMemoryQaModelRepository;
import ru.kuznetsov.qaip.findings.model.FindingsReport;
import ru.kuznetsov.qaip.findings.service.FindingsService;

@Service
public class RegisteredModelFindingsService {

    private final InMemoryQaModelRepository repository;
    private final FindingsService findingsService;
    private final FindingsResponseMapper responseMapper;

    public RegisteredModelFindingsService(
            InMemoryQaModelRepository repository,
            FindingsService findingsService,
            FindingsResponseMapper responseMapper
    ) {
        this.repository = repository;
        this.findingsService = findingsService;
        this.responseMapper = responseMapper;
    }

    public RegisteredModelFindingsResponse analyze(String modelId) {
        JsonNode model = repository.findById(modelId)
                .orElseThrow(() -> new QaModelNotFoundException(modelId));
        FindingsReport report = findingsService.analyze(model);

        if (!report.analyzed()) {
            throw new InvalidQaModelException(report.validation());
        }

        return responseMapper.map(modelId, report);
    }
}
