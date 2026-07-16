package ru.kuznetsov.qaip.coverage.analyzer; import com.fasterxml.jackson.databind.JsonNode; import ru.kuznetsov.qaip.coverage.model.CoverageAnalysisResult;
public interface CoverageAnalyzer { CoverageAnalysisResult analyze(JsonNode qaModel); }
