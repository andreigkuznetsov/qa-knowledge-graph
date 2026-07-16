package ru.kuznetsov.qagraph.storyanalyzer.model;

import jakarta.validation.constraints.NotBlank;

public record StoryAnalysisRequest(
        @NotBlank String projectId,
        @NotBlank String projectName,
        @NotBlank String sourceId,
        @NotBlank String sourceName,
        String externalId,
        @NotBlank String text
) {
}
