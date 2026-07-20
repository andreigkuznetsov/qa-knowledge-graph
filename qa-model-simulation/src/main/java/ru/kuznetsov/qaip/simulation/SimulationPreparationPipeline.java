package ru.kuznetsov.qaip.simulation;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qaip.impact.model.ImpactReport;
import ru.kuznetsov.qaip.simulation.model.TaskMaterializationSet;

final class SimulationPreparationPipeline {
    private final SimulationInputValidator inputValidator =
            new SimulationInputValidator();
    private final CandidateModelMaterializer materializer =
            new CandidateModelMaterializer();

    SimulationPreparation prepareCandidate(
            JsonNode currentModel,
            ImpactReport impactReport,
            TaskMaterializationSet materializationSet
    ) {
        var matches = inputValidator.validateAndMatch(
                currentModel, impactReport, materializationSet);
        CandidateMaterialization materialization =
                materializer.materialize(currentModel, matches);
        return new SimulationPreparation(
                materialization.candidateModel(),
                materializationSet.baseModelFingerprint(),
                materialization.appliedMaterializations()
        );
    }
}
