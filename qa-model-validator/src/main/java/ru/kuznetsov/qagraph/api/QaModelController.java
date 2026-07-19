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
import ru.kuznetsov.qagraph.service.RegisteredModelCoverageService;
import ru.kuznetsov.qagraph.service.RegisteredModelAssessmentService;
import ru.kuznetsov.qagraph.service.RegisteredModelFindingsService;
import ru.kuznetsov.qagraph.service.RegisteredModelRoadmapService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/models")
public class QaModelController {

    private final QaModelRegistrationService registrationService;
    private final QaModelTraceService traceService;
    private final RegisteredModelCoverageService coverageService;
    private final RegisteredModelFindingsService findingsService;
    private final RegisteredModelAssessmentService assessmentService;
    private final RegisteredModelRoadmapService roadmapService;

    public QaModelController(
            QaModelRegistrationService registrationService,
            QaModelTraceService traceService,
            RegisteredModelCoverageService coverageService,
            RegisteredModelFindingsService findingsService,
            RegisteredModelAssessmentService assessmentService,
            RegisteredModelRoadmapService roadmapService
    ) {
        this.registrationService = registrationService;
        this.traceService = traceService;
        this.coverageService = coverageService;
        this.findingsService = findingsService;
        this.assessmentService = assessmentService;
        this.roadmapService = roadmapService;
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

    @GetMapping(
            value = "/{modelId}/coverage",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public RegisteredModelCoverageResponse coverage(
            @PathVariable String modelId
    ) {
        return coverageService.analyze(modelId);
    }

    @GetMapping(
            value = "/{modelId}/findings",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public RegisteredModelFindingsResponse findings(
            @PathVariable String modelId
    ) {
        return findingsService.analyze(modelId);
    }

    @GetMapping(
            value = "/{modelId}/assessment",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public RegisteredModelAssessmentResponse assessment(
            @PathVariable String modelId
    ) {
        return assessmentService.assess(modelId);
    }

    @GetMapping(
            value = "/{modelId}/roadmap",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public RegisteredModelRoadmapResponse roadmap(
            @PathVariable String modelId
    ) {
        return roadmapService.plan(modelId);
    }
}
