package ru.kuznetsov.qaip.explorer.application;

import java.util.Objects;

public record AnalyzeRepositoryCommand(String repositoryPath, String projectName) {
    public AnalyzeRepositoryCommand {
        requireNonBlank(repositoryPath, "repositoryPath");
        requireNonBlank(projectName, "projectName");
    }

    private static void requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
    }
}
