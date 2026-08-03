package ru.kuznetsov.qaip.explorer.application;

public interface AnalyzeRepositoryService {
    AnalyzeRepositoryOutcome analyze(AnalyzeRepositoryCommand command);
}
