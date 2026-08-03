package ru.kuznetsov.qaip.explorer.application;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryRepositoryAnalysisCatalog implements RepositoryAnalysisCatalog {
    private final ConcurrentMap<String, RepositoryAnalysisRecord> records = new ConcurrentHashMap<>();

    @Override
    public void save(RepositoryAnalysisRecord record) {
        Objects.requireNonNull(record, "record");
        records.put(record.repositoryId(), record);
    }

    @Override
    public Optional<RepositoryAnalysisRecord> findByRepositoryId(String repositoryId) {
        return Optional.ofNullable(records.get(Objects.requireNonNull(repositoryId, "repositoryId")));
    }
}
