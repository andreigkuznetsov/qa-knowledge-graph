package ru.kuznetsov.qaip.simulation.api;

import org.springframework.stereotype.Service;
import ru.kuznetsov.qaip.simulation.ModelSimulationEngine;
import ru.kuznetsov.qaip.simulation.model.SimulationResult;

@Service
class SimulationFacade {
    private final ModelSimulationEngine simulationEngine;

    SimulationFacade(ModelSimulationEngine simulationEngine) {
        this.simulationEngine = simulationEngine;
    }

    SimulationResult simulate(SimulationRequest request) {
        return simulationEngine.simulate(
                request.currentModel(),
                request.impactReport(),
                request.taskMaterializationSet()
        );
    }
}
