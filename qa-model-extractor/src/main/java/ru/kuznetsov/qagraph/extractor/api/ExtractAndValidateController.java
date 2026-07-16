package ru.kuznetsov.qagraph.extractor.api;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.kuznetsov.qagraph.extractor.model.ExtractAndValidateResponse;
import ru.kuznetsov.qagraph.extractor.service.ExtractAndValidateService;

@RestController
@RequestMapping("/api/v1/qa-model")
public class ExtractAndValidateController {

    private final ExtractAndValidateService service;

    public ExtractAndValidateController(
            ExtractAndValidateService service
    ) {
        this.service = service;
    }

    @PostMapping(
            value = "/extract-and-validate",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Построить и проверить QA-модель",
            description = "Принимает Story Input Model v0.1 и строит Normalized QA Model v0.1."
    )
    public ResponseEntity<ExtractAndValidateResponse> execute(
            @RequestBody JsonNode input
    ) {
        return ResponseEntity.ok(service.execute(input));
    }
}
