package ru.kuznetsov.qaip.simulation.internal;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qaip.impact.model.ImpactReport;
import ru.kuznetsov.qaip.simulation.model.TaskMaterializationSet;

final class SimulationPreparationPipeline {
    private final SimulationInputValidator inputValidator =
            new SimulationInputValidator();
    private final CandidateModelMaterializer materializer =
            new CandidateModelMaterializer();

    JsonNode prepareCandidate(
            JsonNode currentModel,
            ImpactReport impactReport,
            TaskMaterializationSet materializationSet
    ) {
        var matches = inputValidator.validateAndMatch(
                currentModel, impactReport, materializationSet);
        return materializer.materialize(currentModel, matches);
    }
}
