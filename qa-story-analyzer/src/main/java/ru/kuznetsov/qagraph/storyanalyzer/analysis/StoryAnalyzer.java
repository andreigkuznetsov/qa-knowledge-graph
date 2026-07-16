package ru.kuznetsov.qagraph.storyanalyzer.analysis;

import ru.kuznetsov.qagraph.storyanalyzer.model.StoryAnalysisRequest;
import ru.kuznetsov.qagraph.storyanalyzer.model.StoryAnalysisResult;

public interface StoryAnalyzer {
    StoryAnalysisResult analyze(StoryAnalysisRequest request);
}
