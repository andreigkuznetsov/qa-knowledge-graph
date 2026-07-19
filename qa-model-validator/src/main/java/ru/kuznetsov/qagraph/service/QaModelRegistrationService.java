package ru.kuznetsov.qagraph.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import ru.kuznetsov.qagraph.api.ModelRegistrationResponse;
import ru.kuznetsov.qagraph.registration.ModelDescriptor;
import ru.kuznetsov.qagraph.repository.InMemoryQaModelRepository;
import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;

import java.util.List;

@Service
public class QaModelRegistrationService {

    private final QaModelValidationService validationService;
    private final InMemoryQaModelRepository repository;

    public QaModelRegistrationService(
            QaModelValidationService validationService,
            InMemoryQaModelRepository repository
    ) {
        this.validationService = validationService;
        this.repository = repository;
    }

    public ModelRegistrationResponse register(JsonNode model) {
        QaModelValidationResult validationResult =
                validationService.validate(model);

        if (!validationResult.valid()) {
            throw new InvalidQaModelException(validationResult);
        }

        ModelDescriptor descriptor = repository.save(
                model,
                validationResult.summary().warnings()
        );

        return new ModelRegistrationResponse(
                descriptor.modelId(),
                descriptor.nodeCount(),
                descriptor.relationshipCount(),
                descriptor.warningCount()
        );
    }

    public JsonNode get(String modelId) {
        return repository.findById(modelId)
                .orElseThrow(() ->
                        new QaModelNotFoundException(modelId)
                );
    }

    public List<ModelDescriptor> list() {
        return repository.findAllDescriptors();
    }

    public ModelDescriptor getInfo(String modelId) {
        return repository.findDescriptorById(modelId)
                .orElseThrow(() ->
                        new QaModelNotFoundException(modelId)
                );
    }
}
