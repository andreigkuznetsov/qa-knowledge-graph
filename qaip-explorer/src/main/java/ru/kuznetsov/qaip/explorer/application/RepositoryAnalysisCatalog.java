package ru.kuznetsov.qaip.explorer.application;

import java.util.Optional;

public interface RepositoryAnalysisCatalog {
    void save(RepositoryAnalysisRecord record);

    Optional<RepositoryAnalysisRecord> findByRepositoryId(String repositoryId);
}
