package ru.kuznetsov.qaip.explorer.application;

import ru.kuznetsov.qagraph.extractor.repositoryanalysis.RepositoryAnalysisStatus;

import java.util.List;
import java.util.Objects;

public record AnalyzeRepositoryOutcome(
        String repositoryId,
        String projectIdentity,
        RepositoryAnalysisStatus analysisStatus,
        int discoveredOperationCount,
        List<String> warnings
) {
    public AnalyzeRepositoryOutcome {
        requireNonBlank(repositoryId, "repositoryId");
        requireNonBlank(projectIdentity, "projectIdentity");
        Objects.requireNonNull(analysisStatus, "analysisStatus");
        if (analysisStatus == RepositoryAnalysisStatus.FAILED) {
            throw new IllegalArgumentException("analysisStatus must be COMPLETE or PARTIAL");
        }
        if (discoveredOperationCount < 0) {
            throw new IllegalArgumentException("discoveredOperationCount must not be negative");
        }
        warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings"));
    }

    private static void requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
    }
}
