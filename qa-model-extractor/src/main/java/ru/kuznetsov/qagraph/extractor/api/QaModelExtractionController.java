package ru.kuznetsov.qagraph.extractor.api;

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
import ru.kuznetsov.qagraph.extractor.model.QaModelExtractionResponse;
import ru.kuznetsov.qagraph.extractor.service.QaModelExtractionService;

@RestController
@RequestMapping("/api/v1/qa-model")
public class QaModelExtractionController {

    private final QaModelExtractionService extractionService;

    public QaModelExtractionController(
            QaModelExtractionService extractionService
    ) {
        this.extractionService = extractionService;
    }

    @PostMapping(
            value = "/extract",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Преобразовать Story Input в QA-модель",
            description = "Детерминированно преобразует Story Input Model v0.1 "
                    + "в Normalized QA Model v0.1 без использования LLM."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Story Input Model v0.1",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(type = "object"),
                    examples = @ExampleObject(
                            name = "Минимальная Story Input модель",
                            value = """
                                    {
                                      "schemaVersion": "0.1",
                                      "project": {
                                        "id": "TEST-PROJECT",
                                        "name": "Test project",
                                        "metadata": {}
                                      },
                                      "source": {
                                        "id": "SRC-001",
                                        "type": "USER_STORY",
                                        "name": "US-001",
                                        "metadata": {}
                                      },
                                      "story": {
                                        "externalId": "US-001",
                                        "title": "Изменение статуса",
                                        "actor": "Пользователь",
                                        "goal": "Изменить статус",
                                        "metadata": {}
                                      },
                                      "operations": [
                                        {
                                          "code": "CHANGE_STATUS",
                                          "name": "Изменить статус",
                                          "preconditions": [],
                                          "rules": [],
                                          "scenarios": [
                                            {
                                              "code": "CHANGE_STATUS_SUCCESS",
                                              "title": "Успешная смена статуса",
                                              "tags": ["POSITIVE"],
                                              "given": [],
                                              "when": [
                                                {
                                                  "text": "Пользователь меняет статус",
                                                  "parameters": {}
                                                }
                                              ],
                                              "then": [
                                                {
                                                  "text": "Статус изменён",
                                                  "parameters": {}
                                                }
                                              ],
                                              "coversRuleCodes": [],
                                              "metadata": {}
                                            }
                                          ],
                                          "technicalImplementationRefs": [],
                                          "metadata": {}
                                        }
                                      ],
                                      "technicalImplementations": [],
                                      "metadata": {}
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<QaModelExtractionResponse> extract(
            @RequestBody JsonNode input
    ) {
        return ResponseEntity.ok(extractionService.extract(input));
    }
}
