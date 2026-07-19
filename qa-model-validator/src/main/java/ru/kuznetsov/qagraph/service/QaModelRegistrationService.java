package ru.kuznetsov.qagraph.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import ru.kuznetsov.qagraph.api.ModelRegistrationResponse;
import ru.kuznetsov.qagraph.repository.InMemoryQaModelRepository;
import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;

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

        String modelId = repository.save(model);

        return new ModelRegistrationResponse(
                modelId,
                model.path("nodes").size(),
                model.path("relationships").size(),
                validationResult.summary().warnings()
        );
    }

    public JsonNode get(String modelId) {
        return repository.findById(modelId)
                .orElseThrow(() ->
                        new QaModelNotFoundException(modelId)
                );
    }
}
