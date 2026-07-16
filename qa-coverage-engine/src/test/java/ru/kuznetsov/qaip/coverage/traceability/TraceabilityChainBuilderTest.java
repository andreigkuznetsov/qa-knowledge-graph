package ru.kuznetsov.qaip.coverage.traceability;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

class TraceabilityChainBuilderTest {
    private final ObjectMapper mapper=new ObjectMapper();
    private final TraceabilityChainBuilder builder=new TraceabilityChainBuilder();

    @Test
    void shouldBuildMaximumCompleteAndIncompleteRoutes() throws Exception {
        var result=builder.build(read("traceability-mixed-breaks.json"));
        assertEquals(6,result.totalChains());
        Map<Integer,Long> byDepth=result.chains().stream().collect(
                Collectors.groupingBy(c->c.depth(),Collectors.counting()));
        for(int depth=1;depth<=6;depth++) assertEquals(1L,byDepth.get(depth));
    }

    @Test
    void storyWithoutOperationShouldRemainInResult() throws Exception {
        JsonNode model = mapper.readTree("""
        {
          "nodes": [
            {
              "id": "US-001",
              "type": "USER_STORY",
              "name": "Story"
            }
          ],
          "relationships": []
        }
        """);
        var result=builder.build(model);
        assertEquals(1,result.totalChains());
        assertEquals(1,result.chains().getFirst().depth());
        assertEquals("US-001",result.chains().getFirst().lastNode().id());
        assertNull(result.chains().getFirst().businessOperation());
    }

    private JsonNode read(String name) throws Exception {
        try(InputStream in=new ClassPathResource(name).getInputStream()) { return mapper.readTree(in); }
    }
}
