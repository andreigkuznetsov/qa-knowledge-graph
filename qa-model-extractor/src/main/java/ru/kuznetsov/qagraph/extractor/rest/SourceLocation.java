package ru.kuznetsov.qagraph.extractor.rest;

import java.util.Objects;

public record SourceLocation(String repositoryRelativePath, int line, int column) {
    public SourceLocation {
        Objects.requireNonNull(repositoryRelativePath, "repositoryRelativePath");
        if (repositoryRelativePath.isBlank()) {
            throw new IllegalArgumentException("repositoryRelativePath must not be blank");
        }
        if (line < 1) throw new IllegalArgumentException("line must be positive");
        if (column < 1) throw new IllegalArgumentException("column must be positive");
    }
}
