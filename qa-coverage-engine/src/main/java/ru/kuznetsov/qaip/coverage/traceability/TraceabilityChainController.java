package ru.kuznetsov.qaip.coverage.traceability;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.kuznetsov.qaip.coverage.traceability.model.TraceabilityChainReport;

@RestController
@RequestMapping("/api/v1/coverage/traceability")
public class TraceabilityChainController {

    private final TraceabilityChainService chainService;

    public TraceabilityChainController(
            TraceabilityChainService chainService
    ) {
        this.chainService = chainService;
    }

    @PostMapping(
            value = "/chains",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Построить полные цепочки трассируемости",
            description = "QAIP 0.5A возвращает только полностью завершённые цепочки."
    )
    public ResponseEntity<TraceabilityChainReport> buildChains(
            @RequestBody JsonNode qaModel
    ) {
        return ResponseEntity.ok(chainService.build(qaModel));
    }
}
