package ru.kuznetsov.qaip.explorer.view;

import ru.kuznetsov.qagraph.extractor.repositoryanalysis.RepositoryAnalysisStatus;

import java.util.Objects;

public record RepositorySummaryView(
        String repositoryId,
        String projectIdentity,
        RepositoryAnalysisStatus analysisStatus,
        int operationCount,
        int businessRuleCount,
        int implementationNodeCount,
        int testCount,
        int checkCount,
        int warningCount
) {
    public RepositorySummaryView {
        Objects.requireNonNull(repositoryId, "repositoryId");
        Objects.requireNonNull(projectIdentity, "projectIdentity");
        Objects.requireNonNull(analysisStatus, "analysisStatus");
        requireNonNegative(operationCount, "operationCount");
        requireNonNegative(businessRuleCount, "businessRuleCount");
        requireNonNegative(implementationNodeCount, "implementationNodeCount");
        requireNonNegative(testCount, "testCount");
        requireNonNegative(checkCount, "checkCount");
        requireNonNegative(warningCount, "warningCount");
    }

    private static void requireNonNegative(int value, String field) {
        if (value < 0) throw new IllegalArgumentException(field + " must not be negative");
    }
}
