package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record RepositoryAnalysisResult(
        RepositoryAnalysisStatus status,
        String projectIdentity,
        JsonNode canonicalProjectJson,
        int discoveredOperationCount,
        List<String> warnings,
        Optional<String> failureMessage
) {
    public RepositoryAnalysisResult {
        Objects.requireNonNull(status, "status");
        warnings = immutableWarnings(warnings);
        failureMessage = Objects.requireNonNull(failureMessage, "failureMessage");
        if (discoveredOperationCount < 0) {
            throw new IllegalArgumentException("discoveredOperationCount must not be negative");
        }

        switch (status) {
            case COMPLETE -> {
                requireSuccessfulOutput(projectIdentity, canonicalProjectJson);
                if (!warnings.isEmpty()) {
                    throw new IllegalArgumentException("COMPLETE result must not contain warnings");
                }
                requireNoFailure(failureMessage, status);
            }
            case PARTIAL -> {
                requireSuccessfulOutput(projectIdentity, canonicalProjectJson);
                if (warnings.isEmpty()) {
                    throw new IllegalArgumentException("PARTIAL result must contain warnings");
                }
                requireNoFailure(failureMessage, status);
            }
            case FAILED -> {
                if (projectIdentity != null || canonicalProjectJson != null) {
                    throw new IllegalArgumentException("FAILED result must not contain project output");
                }
                if (discoveredOperationCount != 0) {
                    throw new IllegalArgumentException("FAILED result must have zero discovered operations");
                }
                if (failureMessage.isEmpty() || failureMessage.orElseThrow().isBlank()) {
                    throw new IllegalArgumentException("FAILED result must contain a non-blank failure message");
                }
            }
        }

        canonicalProjectJson = canonicalProjectJson == null ? null : canonicalProjectJson.deepCopy();
    }

    @Override
    public JsonNode canonicalProjectJson() {
        return canonicalProjectJson == null ? null : canonicalProjectJson.deepCopy();
    }

    private static void requireSuccessfulOutput(String projectIdentity, JsonNode canonicalProjectJson) {
        Objects.requireNonNull(projectIdentity, "projectIdentity");
        if (projectIdentity.isBlank()) {
            throw new IllegalArgumentException("projectIdentity must not be blank");
        }
        Objects.requireNonNull(canonicalProjectJson, "canonicalProjectJson");
    }

    private static void requireNoFailure(Optional<String> failureMessage, RepositoryAnalysisStatus status) {
        if (failureMessage.isPresent()) {
            throw new IllegalArgumentException(status + " result must not contain a failure message");
        }
    }

    private static List<String> immutableWarnings(List<String> warnings) {
        Objects.requireNonNull(warnings, "warnings");
        warnings.forEach(warning -> {
            Objects.requireNonNull(warning, "warning");
            if (warning.isBlank()) throw new IllegalArgumentException("warning must not be blank");
        });
        return List.copyOf(warnings);
    }
}
