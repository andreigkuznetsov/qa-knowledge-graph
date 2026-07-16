package ru.kuznetsov.qaip.coverage.traceability.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import ru.kuznetsov.qaip.coverage.traceability.TraceabilityChainBuilder;
import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

class TraceabilityCoverageAnalyzerTest {
    private final ObjectMapper mapper=new ObjectMapper();
    private final TraceabilityChainBuilder builder=new TraceabilityChainBuilder();
    private final TraceabilityCoverageAnalyzer analyzer=new TraceabilityCoverageAnalyzer();

    @Test
    void shouldClassifyEveryBreakAndCalculateCoverage() throws Exception {
        var analysis=analyzer.analyze(builder.build(read("traceability-mixed-breaks.json")));
        assertEquals(6,analysis.summary().totalChains());
        assertEquals(1,analysis.summary().fullyTraceableChains());
        assertEquals(5,analysis.summary().brokenChains());
        assertEquals(16.67,analysis.summary().coveragePercentage());
        assertEquals(3.5,analysis.summary().averageDepth());
        assertEquals(6,analysis.summary().maximumDepth());
        assertEquals(1,analysis.breakdown().fullyTraceable());
        assertEquals(1,analysis.breakdown().brokenAtOperation());
        assertEquals(1,analysis.breakdown().brokenAtRule());
        assertEquals(1,analysis.breakdown().brokenAtScenario());
        assertEquals(1,analysis.breakdown().brokenAtTestImplementation());
        assertEquals(1,analysis.breakdown().brokenAtCheck());
        Set<TraceabilityStatus> statuses=analysis.chains().stream().map(AnalyzedTraceabilityChain::status).collect(Collectors.toSet());
        assertEquals(Set.of(TraceabilityStatus.values()),statuses);
        assertEquals(5,analysis.problems().size());
        assertTrue(analysis.problems().stream().allMatch(p->!p.recommendation().isBlank()));
    }

    private JsonNode read(String name) throws Exception {
        try(InputStream in=new ClassPathResource(name).getInputStream()) { return mapper.readTree(in); }
    }
}
