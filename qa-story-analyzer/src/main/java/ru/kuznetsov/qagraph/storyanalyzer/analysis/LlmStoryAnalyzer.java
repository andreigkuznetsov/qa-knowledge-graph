package ru.kuznetsov.qagraph.storyanalyzer.analysis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import ru.kuznetsov.qagraph.storyanalyzer.llm.LlmClient;
import ru.kuznetsov.qagraph.storyanalyzer.model.StoryAnalysisRequest;
import ru.kuznetsov.qagraph.storyanalyzer.model.StoryAnalysisResult;
import ru.kuznetsov.qagraph.storyanalyzer.prompt.StoryAnalysisPromptFactory;

import java.util.List;

@Service
public class LlmStoryAnalyzer implements StoryAnalyzer {

    private final LlmClient llmClient;
    private final StoryAnalysisPromptFactory promptFactory;
    private final ObjectMapper objectMapper;

    public LlmStoryAnalyzer(
            LlmClient llmClient,
            StoryAnalysisPromptFactory promptFactory,
            ObjectMapper objectMapper
    ) {
        this.llmClient = llmClient;
        this.promptFactory = promptFactory;
        this.objectMapper = objectMapper;
    }

    @Override
    public StoryAnalysisResult analyze(StoryAnalysisRequest request) {
        String content = llmClient.generate(promptFactory.create(request)).content();

        try {
            JsonNode storyInput = objectMapper.readTree(content);
            return new StoryAnalysisResult(true, storyInput, List.of());
        } catch (JsonProcessingException exception) {
            return new StoryAnalysisResult(
                    false,
                    null,
                    List.of("LLM_RESPONSE_IS_NOT_JSON")
            );
        }
    }
}
