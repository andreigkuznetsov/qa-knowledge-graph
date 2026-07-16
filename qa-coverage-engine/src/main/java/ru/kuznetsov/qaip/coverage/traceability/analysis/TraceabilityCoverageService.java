package ru.kuznetsov.qaip.coverage.traceability.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import ru.kuznetsov.qagraph.validationcore.QaModelValidationEngine;
import ru.kuznetsov.qaip.coverage.traceability.TraceabilityChainBuilder;

import java.time.Instant;
import java.util.List;

@Service
public class TraceabilityCoverageService {
    private static final String RELEASE = "0.5B";
    private final QaModelValidationEngine validationEngine;
    private final TraceabilityChainBuilder builder;
    private final TraceabilityCoverageAnalyzer analyzer;

    public TraceabilityCoverageService(QaModelValidationEngine validationEngine,
            TraceabilityChainBuilder builder, TraceabilityCoverageAnalyzer analyzer) {
        this.validationEngine=validationEngine; this.builder=builder; this.analyzer=analyzer;
    }

    public TraceabilityCoverageReport analyze(JsonNode qaModel) {
        var validation=validationEngine.validate(qaModel);
        if (!validation.valid()) {
            return new TraceabilityCoverageReport(false,RELEASE,validation.schemaVersion(),
                    Instant.now(),null,null,List.of(),List.of(),validation);
        }
        var analysis=analyzer.analyze(builder.build(qaModel));
        return new TraceabilityCoverageReport(true,RELEASE,validation.schemaVersion(),
                Instant.now(),analysis.summary(),analysis.breakdown(),
                analysis.chains(),analysis.problems(),validation);
    }
}
