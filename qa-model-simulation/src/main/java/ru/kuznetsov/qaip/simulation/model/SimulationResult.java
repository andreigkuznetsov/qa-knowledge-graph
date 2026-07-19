package ru.kuznetsov.qaip.simulation.model;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;

import java.util.List;
import java.util.Objects;

public record SimulationResult(
        String simulationContractVersion,
        String baseModelFingerprint,
        String futureModelFingerprint,
        JsonNode futureModel,
        List<AppliedMaterialization> appliedMaterializations,
        QaModelValidationResult validation
) {
    public static final String CONTRACT_VERSION = "0.1";

    public SimulationResult {
        requireNonBlank(simulationContractVersion,
                "simulationContractVersion");
        requireNonBlank(baseModelFingerprint, "baseModelFingerprint");
        requireNonBlank(futureModelFingerprint, "futureModelFingerprint");
        Objects.requireNonNull(futureModel, "futureModel must not be null");
        Objects.requireNonNull(appliedMaterializations,
                "appliedMaterializations must not be null");
        Objects.requireNonNull(validation, "validation must not be null");
        futureModel = futureModel.deepCopy();
        appliedMaterializations = List.copyOf(appliedMaterializations);
    }

    @Override
    public JsonNode futureModel() {
        return futureModel.deepCopy();
    }

    private static void requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
