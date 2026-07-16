package ru.kuznetsov.qaip.coverage.traceability.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/coverage/traceability")
public class TraceabilityCoverageController {
    private final TraceabilityCoverageService service;
    public TraceabilityCoverageController(TraceabilityCoverageService service) {
        this.service=service;
    }

    @PostMapping(consumes=MediaType.APPLICATION_JSON_VALUE,
            produces=MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary="Проанализировать полную трассируемость",
            description="Строит максимальные маршруты, определяет FULLY_TRACEABLE и BROKEN_AT_*.")
    public ResponseEntity<TraceabilityCoverageReport> analyze(@RequestBody JsonNode model) {
        return ResponseEntity.ok(service.analyze(model));
    }
}
