package ru.kuznetsov.qagraph.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.kuznetsov.qagraph.registration.ModelDescriptor;
import ru.kuznetsov.qagraph.service.QaModelRegistrationService;
import ru.kuznetsov.qagraph.service.QaModelTraceService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/models")
public class QaModelController {

    private final QaModelRegistrationService registrationService;
    private final QaModelTraceService traceService;

    public QaModelController(
            QaModelRegistrationService registrationService,
            QaModelTraceService traceService
    ) {
        this.registrationService = registrationService;
        this.traceService = traceService;
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

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ModelDescriptor> list() {
        return registrationService.list();
    }

    @GetMapping(
            value = "/{modelId}/info",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ModelDescriptor getInfo(@PathVariable String modelId) {
        return registrationService.getInfo(modelId);
    }

    @GetMapping(
            value = "/{modelId}/trace",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public TraceResponse trace(
            @PathVariable String modelId,
            @RequestParam(name = "from", required = false)
            String fromNodeId,
            @RequestParam(name = "to", required = false)
            String toNodeId
    ) {
        return traceService.trace(
                modelId,
                fromNodeId,
                toNodeId
        );
    }
}
