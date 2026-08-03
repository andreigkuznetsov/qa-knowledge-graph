package ru.kuznetsov.qaip.explorer.application;

import ru.kuznetsov.qagraph.extractor.repositoryanalysis.RepositoryAnalysisStatus;

import java.util.List;
import java.util.Objects;

public record RepositoryAnalysisRecord(
        String repositoryId,
        String projectIdentity,
        RepositoryAnalysisStatus analysisStatus,
        int operationCount,
        List<String> warnings
) {
    public RepositoryAnalysisRecord {
        Objects.requireNonNull(repositoryId, "repositoryId");
        Objects.requireNonNull(projectIdentity, "projectIdentity");
        Objects.requireNonNull(analysisStatus, "analysisStatus");
        warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings"));
    }
}
