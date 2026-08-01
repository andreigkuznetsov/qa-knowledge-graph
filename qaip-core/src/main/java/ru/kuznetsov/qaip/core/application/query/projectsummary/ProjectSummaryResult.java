package ru.kuznetsov.qaip.core.application.query.projectsummary;

import java.util.Objects;

public record ProjectSummaryResult(
        String projectId,
        String projectContractVersion,
        String schemaVersion,
        int sourceCount,
        int nodeCount,
        int relationshipCount,
        int evidenceCount,
        int declaredChangeCount) {

    public ProjectSummaryResult {
        projectId = requireText(projectId, "projectId");
        projectContractVersion = requireText(projectContractVersion, "projectContractVersion");
        schemaVersion = requireText(schemaVersion, "schemaVersion");
        requireNonNegative(sourceCount, "sourceCount");
        requireNonNegative(nodeCount, "nodeCount");
        requireNonNegative(relationshipCount, "relationshipCount");
        requireNonNegative(evidenceCount, "evidenceCount");
        requireNonNegative(declaredChangeCount, "declaredChangeCount");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) throw new IllegalArgumentException(name + " must not be negative");
    }
}
