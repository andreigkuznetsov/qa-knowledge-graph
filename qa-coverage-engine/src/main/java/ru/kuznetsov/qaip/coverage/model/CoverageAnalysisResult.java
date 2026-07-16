package ru.kuznetsov.qaip.coverage.model; import java.util.List;
public record CoverageAnalysisResult(List<CoverageMetric> metrics,List<CoverageProblem> problems){}
