package ru.kuznetsov.qagraph.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import ru.kuznetsov.qagraph.validationcore.QaModelValidationEngine;
import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;

@Service
public class QaModelValidationService {

    private final QaModelValidationEngine validationEngine;

    public QaModelValidationService(
            QaModelValidationEngine validationEngine
    ) {
        this.validationEngine = validationEngine;
    }

    public QaModelValidationResult validate(JsonNode document) {
        return validationEngine.validate(document);
    }
}