package ru.kuznetsov.qagraph.api;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.kuznetsov.qagraph.service.QaModelValidationService;
import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;

@RestController
@RequestMapping("/api/v1/qa-model")
public class QaModelValidationController {

    private final QaModelValidationService validationService;

    public QaModelValidationController(
            QaModelValidationService validationService
    ) {
        this.validationService = validationService;
    }

    @PostMapping(
            value = "/validate",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Проверить нормализованную QA-модель",
            description = """
                    Выполняет проверку QA Knowledge Graph по JSON Schema,
                    а затем запускает семантическую валидацию узлов,
                    связей, источников и тестового покрытия.
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Normalized QA Model v0.1",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(type = "object"),
                    examples = @ExampleObject(
                            name = "Минимальная валидная модель",
                            value = """
                                    {
                                      "schemaVersion": "0.1",
                                      "project": {
                                        "id": "TEST-PROJECT",
                                        "name": "QA Knowledge Graph Test",
                                        "metadata": {}
                                      },
                                      "sources": [],
                                      "nodes": [],
                                      "relationships": []
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<QaModelValidationResult> validate(
            @RequestBody JsonNode document
    ) {
        return ResponseEntity.ok(
                validationService.validate(document)
        );
    }
}