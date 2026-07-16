package ru.kuznetsov.qagraph.storyanalyzer.prompt;

import org.springframework.stereotype.Component;
import ru.kuznetsov.qagraph.storyanalyzer.llm.LlmRequest;
import ru.kuznetsov.qagraph.storyanalyzer.model.StoryAnalysisRequest;

@Component
public class StoryAnalysisPromptFactory {

    public LlmRequest create(StoryAnalysisRequest request) {
        return new LlmRequest(systemPrompt(), userPrompt(request));
    }

    private String systemPrompt() {
        return """
                Ты выполняешь структурный анализ требований для QA Knowledge Graph.
                Преобразуй исходный текст User Story или Use Case в Story Input JSON.
                Используй только факты из исходного текста.
                Не придумывай API, БД, SQL, UI-компоненты, правила, статусы и ограничения.
                Выдели actor, goal, businessValue, operations, preconditions, rules и scenarios.
                Для сценариев сформируй given, when и then.
                Верни только JSON без Markdown и пояснений.
                """;
    }

    private String userPrompt(StoryAnalysisRequest request) {
        return """
                PROJECT_ID: %s
                PROJECT_NAME: %s
                SOURCE_ID: %s
                SOURCE_NAME: %s
                EXTERNAL_ID: %s

                REQUIREMENTS_TEXT:
                %s
                """.formatted(
                request.projectId(),
                request.projectName(),
                request.sourceId(),
                request.sourceName(),
                request.externalId(),
                request.text()
        );
    }
}
