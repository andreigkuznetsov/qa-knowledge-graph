package ru.kuznetsov.qagraph.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.repository.InMemoryQaModelRepository;
import ru.kuznetsov.qagraph.registration.ModelDescriptor;
import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;
import ru.kuznetsov.qagraph.validationcore.model.ValidationSummary;

import java.util.List;
import java.time.Instant;

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
        assertEquals(
                response.modelId(),
                service.getInfo(response.modelId()).modelId()
        );
        assertEquals(1, service.list().size());
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
        verify(repository, never()).save(model, 0);
    }

    @Test
    void shouldDelegateDescriptorQueriesToRepository() {
        QaModelValidationService validationService =
                mock(QaModelValidationService.class);
        InMemoryQaModelRepository repository =
                mock(InMemoryQaModelRepository.class);
        QaModelRegistrationService service =
                new QaModelRegistrationService(
                        validationService,
                        repository
                );
        ModelDescriptor descriptor = new ModelDescriptor(
                "model-1",
                Instant.parse("2026-07-19T10:00:00Z"),
                1,
                2,
                3
        );
        when(repository.findAllDescriptors())
                .thenReturn(List.of(descriptor));
        when(repository.findDescriptorById("model-1"))
                .thenReturn(java.util.Optional.of(descriptor));

        assertEquals(List.of(descriptor), service.list());
        assertEquals(descriptor, service.getInfo("model-1"));
    }

    @Test
    void shouldThrowWhenDescriptorDoesNotExist() {
        QaModelValidationService validationService =
                mock(QaModelValidationService.class);
        InMemoryQaModelRepository repository =
                mock(InMemoryQaModelRepository.class);
        QaModelRegistrationService service =
                new QaModelRegistrationService(
                        validationService,
                        repository
                );
        when(repository.findDescriptorById("unknown"))
                .thenReturn(java.util.Optional.empty());

        assertThrows(
                QaModelNotFoundException.class,
                () -> service.getInfo("unknown")
        );
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
