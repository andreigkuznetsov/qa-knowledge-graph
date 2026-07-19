package ru.kuznetsov.qagraph.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import ru.kuznetsov.qagraph.api.FindingResponse;
import ru.kuznetsov.qagraph.api.FindingsSummaryResponse;
import ru.kuznetsov.qagraph.api.RegisteredModelFindingsResponse;
import ru.kuznetsov.qagraph.repository.InMemoryQaModelRepository;
import ru.kuznetsov.qaip.findings.model.Finding;
import ru.kuznetsov.qaip.findings.model.FindingsReport;
import ru.kuznetsov.qaip.findings.model.FindingsSummary;
import ru.kuznetsov.qaip.findings.service.FindingsService;

@Service
public class RegisteredModelFindingsService {

    private final InMemoryQaModelRepository repository;
    private final FindingsService findingsService;

    public RegisteredModelFindingsService(
            InMemoryQaModelRepository repository,
            FindingsService findingsService
    ) {
        this.repository = repository;
        this.findingsService = findingsService;
    }

    public RegisteredModelFindingsResponse analyze(String modelId) {
        JsonNode model = repository.findById(modelId)
                .orElseThrow(() -> new QaModelNotFoundException(modelId));
        FindingsReport report = findingsService.analyze(model);

        if (!report.analyzed()) {
            throw new InvalidQaModelException(report.validation());
        }

        return new RegisteredModelFindingsResponse(
                modelId,
                true,
                report.schemaVersion(),
                toResponse(report.summary()),
                report.findings().stream()
                        .map(this::toResponse)
                        .toList(),
                report.validation()
        );
    }

    private FindingsSummaryResponse toResponse(FindingsSummary summary) {
        return new FindingsSummaryResponse(
                summary.total(),
                summary.high(),
                summary.medium(),
                summary.low()
        );
    }

    private FindingResponse toResponse(Finding finding) {
        return new FindingResponse(
                finding.code().name(),
                finding.severity().name(),
                finding.nodeId(),
                finding.nodeType(),
                finding.message(),
                finding.recommendation()
        );
    }
}
