package ru.kuznetsov.qaip.explorer.api;

import java.util.List;

public record AnalyzeRepositoryResponse(
        String repositoryId,
        String projectIdentity,
        String analysisStatus,
        int discoveredOperationCount,
        List<String> warnings
) {
    public AnalyzeRepositoryResponse {
        warnings = List.copyOf(warnings);
    }
}
