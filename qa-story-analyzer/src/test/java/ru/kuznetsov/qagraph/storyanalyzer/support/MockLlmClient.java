package ru.kuznetsov.qagraph.storyanalyzer.support;

import ru.kuznetsov.qagraph.storyanalyzer.llm.*;

public class MockLlmClient implements LlmClient {

    private final String response;

    public MockLlmClient(String response) {
        this.response = response;
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
        return new LlmResponse(response);
    }
}
