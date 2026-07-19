package ru.kuznetsov.qagraph.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.api.FindingsResponseMapper;
import ru.kuznetsov.qagraph.repository.InMemoryQaModelRepository;
import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;
import ru.kuznetsov.qagraph.validationcore.model.ValidationSummary;
import ru.kuznetsov.qaip.findings.model.Finding;
import ru.kuznetsov.qaip.findings.model.FindingCode;
import ru.kuznetsov.qaip.findings.model.FindingSeverity;
import ru.kuznetsov.qaip.findings.model.FindingsReport;
import ru.kuznetsov.qaip.findings.model.FindingsSummary;
import ru.kuznetsov.qaip.findings.service.FindingsService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegisteredModelFindingsServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldLoadDelegateAndMapDomainReportWithoutMutation()
            throws Exception {
        InMemoryQaModelRepository repository = new InMemoryQaModelRepository();
        JsonNode model = objectMapper.readTree(
                "{\"nodes\":[{}],\"relationships\":[]}"
        );
        String modelId = repository.save(model, 0).modelId();
        FindingsService findingsService = mock(FindingsService.class);
        when(findingsService.analyze(any(JsonNode.class)))
                .thenReturn(validReport());
        RegisteredModelFindingsService service =
                new RegisteredModelFindingsService(
                        repository,
                        findingsService,
                        new FindingsResponseMapper()
                );

        var response = service.analyze(modelId);

        assertEquals(modelId, response.modelId());
        assertEquals(1, response.summary().total());
        assertEquals("BUSINESS_RULE_WITHOUT_SCENARIO", response.findings().getFirst().code());
        assertEquals("HIGH", response.findings().getFirst().severity());
        assertEquals(model, repository.findById(modelId).orElseThrow());
        verify(findingsService).analyze(any(JsonNode.class));
    }

    @Test
    void unknownModelShouldUseExistingException() {
        RegisteredModelFindingsService service =
                new RegisteredModelFindingsService(
                        new InMemoryQaModelRepository(),
                        mock(FindingsService.class),
                        new FindingsResponseMapper()
                );

        assertThrows(
                QaModelNotFoundException.class,
                () -> service.analyze("unknown")
        );
    }

    @Test
    void invalidStoredModelShouldUseExistingValidationException()
            throws Exception {
        InMemoryQaModelRepository repository = new InMemoryQaModelRepository();
        String modelId = repository.save(
                objectMapper.readTree("{}"),
                0
        ).modelId();
        FindingsService findingsService = mock(FindingsService.class);
        QaModelValidationResult validation = validation(false);
        when(findingsService.analyze(any(JsonNode.class))).thenReturn(new FindingsReport(
                false,
                null,
                new FindingsSummary(0, 0, 0, 0),
                List.of(),
                validation
        ));
        RegisteredModelFindingsService service =
                new RegisteredModelFindingsService(
                        repository,
                        findingsService,
                        new FindingsResponseMapper()
                );

        InvalidQaModelException exception = assertThrows(
                InvalidQaModelException.class,
                () -> service.analyze(modelId)
        );

        assertEquals(validation, exception.validationResult());
    }

    private FindingsReport validReport() {
        Finding finding = new Finding(
                FindingCode.BUSINESS_RULE_WITHOUT_SCENARIO,
                FindingSeverity.HIGH,
                "BR-002",
                "BUSINESS_RULE",
                "Business rule BR-002 is not covered by any scenario.",
                "Add a scenario."
        );
        return new FindingsReport(
                true,
                "0.1",
                new FindingsSummary(1, 1, 0, 0),
                List.of(finding),
                validation(true)
        );
    }

    private QaModelValidationResult validation(boolean valid) {
        int errors = valid ? 0 : 1;
        return new QaModelValidationResult(
                valid,
                valid ? "0.1" : null,
                new ValidationSummary(errors, 0, errors),
                List.of()
        );
    }
}
