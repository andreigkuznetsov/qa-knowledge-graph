package ru.kuznetsov.qaip.simulation.model;

import java.util.List;
import java.util.Objects;

public record TaskMaterializationSet(
        String materializationContractVersion,
        String baseModelFingerprint,
        List<TaskMaterialization> materializations
) {
    public static final String SUPPORTED_CONTRACT_VERSION = "0.1";

    public TaskMaterializationSet {
        requireNonBlank(materializationContractVersion,
                "materializationContractVersion");
        requireNonBlank(baseModelFingerprint, "baseModelFingerprint");
        Objects.requireNonNull(materializations,
                "materializations must not be null");
        materializations = List.copyOf(materializations);
    }

    private static void requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
