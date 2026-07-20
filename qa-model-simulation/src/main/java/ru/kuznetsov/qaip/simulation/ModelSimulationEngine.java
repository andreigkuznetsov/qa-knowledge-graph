package ru.kuznetsov.qaip.simulation;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qagraph.validationcore.QaModelValidationEngine;
import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;
import ru.kuznetsov.qaip.impact.model.ImpactReport;
import ru.kuznetsov.qaip.simulation.error.SimulationErrorCode;
import ru.kuznetsov.qaip.simulation.error.SimulationException;
import ru.kuznetsov.qaip.simulation.model.SimulationResult;
import ru.kuznetsov.qaip.simulation.model.TaskMaterializationSet;

/**
 * Stateless deterministic orchestration boundary for QA-model simulation.
 */
public final class ModelSimulationEngine {
    private final SimulationPreparationPipeline preparationPipeline;
    private final QaModelValidationEngine validationEngine;
    private final QaModelFingerprintCalculator fingerprintCalculator;

    public ModelSimulationEngine() {
        this(
                new SimulationPreparationPipeline(),
                new QaModelValidationEngine(),
                new QaModelFingerprintCalculator()
        );
    }

    ModelSimulationEngine(
            SimulationPreparationPipeline preparationPipeline,
            QaModelValidationEngine validationEngine,
            QaModelFingerprintCalculator fingerprintCalculator
    ) {
        this.preparationPipeline = preparationPipeline;
        this.validationEngine = validationEngine;
        this.fingerprintCalculator = fingerprintCalculator;
    }

    public SimulationResult simulate(
            JsonNode currentModel,
            ImpactReport impactReport,
            TaskMaterializationSet materializationSet
    ) {
        SimulationPreparation preparation =
                preparationPipeline.prepareCandidate(
                        currentModel, impactReport, materializationSet);
        JsonNode candidateModel = preparation.candidateModel();
        QaModelValidationResult validation =
                validationEngine.validate(candidateModel);
        if (!validation.valid()) {
            throw new SimulationException(
                    SimulationErrorCode.CANDIDATE_MODEL_VALIDATION_FAILED,
                    "Candidate QA model failed validation",
                    null, null, validation);
        }

        String futureModelFingerprint =
                fingerprintCalculator.calculate(candidateModel);
        return new SimulationResult(
                SimulationResult.CONTRACT_VERSION,
                preparation.verifiedBaseModelFingerprint(),
                futureModelFingerprint,
                candidateModel,
                preparation.appliedMaterializations(),
                validation
        );
    }
}
