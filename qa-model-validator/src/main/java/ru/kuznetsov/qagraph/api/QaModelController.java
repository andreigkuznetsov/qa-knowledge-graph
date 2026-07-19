package ru.kuznetsov.qagraph.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.kuznetsov.qagraph.service.QaModelRegistrationService;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/models")
public class QaModelController {

    private final QaModelRegistrationService registrationService;

    public QaModelController(
            QaModelRegistrationService registrationService
    ) {
        this.registrationService = registrationService;
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ModelRegistrationResponse> register(
            @RequestBody JsonNode model
    ) {
        ModelRegistrationResponse response =
                registrationService.register(model);

        return ResponseEntity.created(
                        URI.create("/api/v1/models/" + response.modelId())
                )
                .body(response);
    }

    @GetMapping(
            value = "/{modelId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public JsonNode get(@PathVariable String modelId) {
        return registrationService.get(modelId);
    }
}
