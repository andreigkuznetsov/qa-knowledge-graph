package ru.kuznetsov.qaip.coverage.traceability;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import ru.kuznetsov.qagraph.validationcore.QaModelValidationEngine;
import ru.kuznetsov.qaip.coverage.traceability.model.TraceabilityChainReport;

import java.time.Instant;
import java.util.List;

@Service
public class TraceabilityChainService {

    private static final String RELEASE = "0.5B";

    private final QaModelValidationEngine validationEngine;
    private final TraceabilityChainBuilder chainBuilder;

    public TraceabilityChainService(
            QaModelValidationEngine validationEngine,
            TraceabilityChainBuilder chainBuilder
    ) {
        this.validationEngine = validationEngine;
        this.chainBuilder = chainBuilder;
    }

    public TraceabilityChainReport build(JsonNode qaModel) {
        var validation = validationEngine.validate(qaModel);

        if (!validation.valid()) {
            return new TraceabilityChainReport(
                    false,
                    RELEASE,
                    validation.schemaVersion(),
                    Instant.now(),
                    0,
                    List.of(),
                    validation
            );
        }

        var result = chainBuilder.build(qaModel);

        return new TraceabilityChainReport(
                true,
                RELEASE,
                validation.schemaVersion(),
                Instant.now(),
                result.totalChains(),
                result.chains(),
                validation
        );
    }
}
