package ru.kuznetsov.qagraph.storyanalyzer.llm;

public interface LlmClient {
    LlmResponse generate(LlmRequest request);
}
