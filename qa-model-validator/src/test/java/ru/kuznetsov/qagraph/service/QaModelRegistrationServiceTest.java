package ru.kuznetsov.qagraph.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.repository.InMemoryQaModelRepository;
import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;
import ru.kuznetsov.qagraph.validationcore.model.ValidationSummary;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QaModelRegistrationServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldPersistValidModelAndExposeCounts() throws Exception {
        QaModelValidationService validationService =
                mock(QaModelValidationService.class);
        InMemoryQaModelRepository repository =
                new InMemoryQaModelRepository();
        QaModelRegistrationService service =
                new QaModelRegistrationService(
                        validationService,
                        repository
                );
        JsonNode model = objectMapper.readTree(
                "{\"nodes\":[{}],\"relationships\":[{},{}]}"
        );
        when(validationService.validate(model)).thenReturn(
                validationResult(true, 2)
        );

        var response = service.register(model);

        assertEquals(1, response.nodeCount());
        assertEquals(2, response.relationshipCount());
        assertEquals(2, response.warnings());
        assertEquals(model, service.get(response.modelId()));
    }

    @Test
    void shouldNotPersistInvalidModel() throws Exception {
        QaModelValidationService validationService =
                mock(QaModelValidationService.class);
        InMemoryQaModelRepository repository =
                mock(InMemoryQaModelRepository.class);
        QaModelRegistrationService service =
                new QaModelRegistrationService(
                        validationService,
                        repository
                );
        JsonNode model = objectMapper.readTree("{}");
        when(validationService.validate(model)).thenReturn(
                validationResult(false, 0)
        );

        assertThrows(
                InvalidQaModelException.class,
                () -> service.register(model)
        );
        verify(repository, never()).save(model);
    }

    private QaModelValidationResult validationResult(
            boolean valid,
            int warnings
    ) {
        int errors = valid ? 0 : 1;
        return new QaModelValidationResult(
                valid,
                "0.1",
                new ValidationSummary(
                        errors,
                        warnings,
                        errors + warnings
                ),
                List.of()
        );
    }
}
