package ru.kuznetsov.qagraph.extractor.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import ru.kuznetsov.qagraph.extractor.model.ExtractAndValidateResponse;
import ru.kuznetsov.qagraph.extractor.model.QaModelExtractionResponse;
import ru.kuznetsov.qagraph.validationcore.QaModelValidationEngine;

@Service
public class ExtractAndValidateService {

    private final QaModelExtractionService extractionService;
    private final QaModelValidationEngine validationEngine;

    public ExtractAndValidateService(
            QaModelExtractionService extractionService,
            QaModelValidationEngine validationEngine
    ) {
        this.extractionService = extractionService;
        this.validationEngine = validationEngine;
    }

    public ExtractAndValidateResponse execute(JsonNode input) {
        QaModelExtractionResponse extraction =
                extractionService.extract(input);

        var extractionResult =
                new ExtractAndValidateResponse.ExtractionResult(
                        extraction.extracted(),
                        extraction.summary().errors(),
                        extraction.summary().warnings()
                );

        if (!extraction.extracted()) {
            return new ExtractAndValidateResponse(
                    false,
                    extractionResult,
                    null,
                    null
            );
        }

        var validation = validationEngine.validate(
                extraction.qaModel()
        );

        var validationResult =
                new ExtractAndValidateResponse.ValidationResult(
                        validation.valid(),
                        validation.schemaVersion(),
                        validation.summary().errors(),
                        validation.summary().warnings(),
                        validation.issues()
                );

        return new ExtractAndValidateResponse(
                validation.valid(),
                extractionResult,
                validationResult,
                extraction.qaModel()
        );
    }
}
