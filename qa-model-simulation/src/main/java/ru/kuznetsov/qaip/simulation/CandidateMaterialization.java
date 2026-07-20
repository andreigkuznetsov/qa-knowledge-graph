package ru.kuznetsov.qaip.simulation;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qaip.simulation.model.AppliedMaterialization;

import java.util.List;
import java.util.Objects;

record CandidateMaterialization(
        JsonNode candidateModel,
        List<AppliedMaterialization> appliedMaterializations
) {
    CandidateMaterialization {
        Objects.requireNonNull(candidateModel,
                "candidateModel must not be null");
        Objects.requireNonNull(appliedMaterializations,
                "appliedMaterializations must not be null");
        candidateModel = candidateModel.deepCopy();
        appliedMaterializations = List.copyOf(appliedMaterializations);
    }

    @Override
    public JsonNode candidateModel() {
        return candidateModel.deepCopy();
    }
}
