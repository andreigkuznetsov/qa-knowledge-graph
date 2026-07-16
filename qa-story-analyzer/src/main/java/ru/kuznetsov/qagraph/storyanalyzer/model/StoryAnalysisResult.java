package ru.kuznetsov.qagraph.storyanalyzer.model;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record StoryAnalysisResult(
        boolean analyzed,
        JsonNode storyInput,
        List<String> warnings
) {
}
