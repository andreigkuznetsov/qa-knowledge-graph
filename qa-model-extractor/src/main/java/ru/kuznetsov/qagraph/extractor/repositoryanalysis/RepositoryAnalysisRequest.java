package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import java.nio.file.Path;
import java.util.Objects;

public record RepositoryAnalysisRequest(Path repositoryRoot, String projectName) {
    public RepositoryAnalysisRequest {
        Objects.requireNonNull(repositoryRoot, "repositoryRoot");
        Objects.requireNonNull(projectName, "projectName");
        if (projectName.isBlank()) throw new IllegalArgumentException("projectName must not be blank");
    }
}
