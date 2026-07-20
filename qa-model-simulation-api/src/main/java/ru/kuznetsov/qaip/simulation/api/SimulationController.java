package ru.kuznetsov.qaip.simulation.api;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.kuznetsov.qaip.simulation.model.SimulationResult;

@RestController
@RequestMapping("/api/v1/simulation")
class SimulationController {
    private final SimulationFacade simulationFacade;

    SimulationController(SimulationFacade simulationFacade) {
        this.simulationFacade = simulationFacade;
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Simulate deterministic QA-model remediation")
    ResponseEntity<SimulationResult> simulate(
            @RequestBody SimulationRequest request
    ) {
        return ResponseEntity.ok(simulationFacade.simulate(request));
    }
}
