package ru.kuznetsov.qaip.coverage.model;
import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult; import java.time.Instant; import java.util.List;
public record CoverageReport(boolean analyzed,String release,String schemaVersion,Instant generatedAt,CoverageSummary summary,List<CoverageMetric> metrics,List<CoverageProblem> problems,QaModelValidationResult validation){}
