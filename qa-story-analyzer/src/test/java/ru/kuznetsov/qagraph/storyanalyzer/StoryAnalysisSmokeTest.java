package ru.kuznetsov.qagraph.storyanalyzer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.storyanalyzer.analysis.LlmStoryAnalyzer;
import ru.kuznetsov.qagraph.storyanalyzer.model.*;
import ru.kuznetsov.qagraph.storyanalyzer.prompt.StoryAnalysisPromptFactory;
import ru.kuznetsov.qagraph.storyanalyzer.support.MockLlmClient;

import static org.junit.jupiter.api.Assertions.*;

class StoryAnalysisSmokeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StoryAnalysisPromptFactory promptFactory =
            new StoryAnalysisPromptFactory();

    @Test
    void validLlmJsonShouldBeParsed() {
        MockLlmClient llmClient = new MockLlmClient("""
                {
                  "schemaVersion": "0.1",
                  "project": {
                    "id": "DPD-SVERKA",
                    "name": "ИС Сверка",
                    "metadata": {}
                  },
                  "source": {
                    "id": "SRC-SORT-385",
                    "type": "USE_CASE",
                    "name": "SORT-385",
                    "metadata": {}
                  },
                  "story": {
                    "externalId": "SORT-385",
                    "title": "Сменить статус сверки"
                  },
                  "operations": [],
                  "technicalImplementations": []
                }
                """);

        LlmStoryAnalyzer analyzer =
                new LlmStoryAnalyzer(llmClient, promptFactory, objectMapper);

        StoryAnalysisResult result = analyzer.analyze(request());

        assertTrue(result.analyzed());
        assertNotNull(result.storyInput());
        assertEquals(
                "0.1",
                result.storyInput().path("schemaVersion").asText()
        );
    }

    @Test
    void invalidLlmResponseShouldReturnWarning() {
        MockLlmClient llmClient =
                new MockLlmClient("Я проанализировал требования...");

        LlmStoryAnalyzer analyzer =
                new LlmStoryAnalyzer(llmClient, promptFactory, objectMapper);

        StoryAnalysisResult result = analyzer.analyze(request());

        assertFalse(result.analyzed());
        assertTrue(result.warnings().contains("LLM_RESPONSE_IS_NOT_JSON"));
    }

    private StoryAnalysisRequest request() {
        return new StoryAnalysisRequest(
                "DPD-SVERKA",
                "ИС Сверка",
                "SRC-SORT-385",
                "SORT-385 — Статусная модель сверки",
                "SORT-385",
                "Я как сотрудник автозагрузки хочу менять статус сверки."
        );
    }
}
