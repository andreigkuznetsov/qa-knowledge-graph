package ru.kuznetsov.qagraph.trace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceEngineTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TraceEngine traceEngine = new TraceEngine();

    @Test
    void shouldFindDirectRelationship() throws Exception {
        TracePath path = traceEngine.trace(
                model("A,B", "R1:A:LINKS:B"),
                "A",
                "B"
        );

        assertTrue(path.found());
        assertEquals(1, path.relationships().size());
        assertEquals("A", path.nodes().get(0).id());
        assertEquals("B", path.nodes().get(1).id());
    }

    @Test
    void shouldFindPathThroughMultipleRelationships() throws Exception {
        TracePath path = traceEngine.trace(
                model("A,B,C", "R1:A:ONE:B,R2:B:TWO:C"),
                "A",
                "C"
        );

        assertEquals(
                java.util.List.of("A", "B", "C"),
                path.nodes().stream().map(TraceNode::id).toList()
        );
        assertEquals(2, path.relationships().size());
    }

    @Test
    void shouldChooseShortestPath() throws Exception {
        TracePath path = traceEngine.trace(
                model(
                        "A,B,C,D",
                        "R1:A:ONE:B,R2:B:TWO:C,R3:C:THREE:D,R4:A:DIRECT:D"
                ),
                "A",
                "D"
        );

        assertEquals(1, path.relationships().size());
        assertEquals("DIRECT", path.relationships().getFirst().type());
    }

    @Test
    void shouldReturnNotFoundWhenNoDirectedPathExists()
            throws Exception {
        TracePath path = traceEngine.trace(
                model("A,B", ""),
                "A",
                "B"
        );

        assertFalse(path.found());
        assertTrue(path.nodes().isEmpty());
        assertTrue(path.relationships().isEmpty());
    }

    @Test
    void shouldHandleCyclesWithoutRevisitingNodes() throws Exception {
        TracePath path = traceEngine.trace(
                model(
                        "A,B,C",
                        "R1:A:FORWARD:B,R2:B:BACK:A,R3:B:TARGET:C"
                ),
                "A",
                "C"
        );

        assertTrue(path.found());
        assertEquals(2, path.relationships().size());
    }

    @Test
    void shouldReturnZeroLengthPathForSameNode() throws Exception {
        TracePath path = traceEngine.trace(
                model("A", ""),
                "A",
                "A"
        );

        assertTrue(path.found());
        assertEquals(0, path.relationships().size());
        assertEquals(1, path.nodes().size());
    }

    @Test
    void shouldUseSourceRelationshipOrderForEqualShortestPaths()
            throws Exception {
        TracePath path = traceEngine.trace(
                model(
                        "A,B,C,D",
                        "R1:A:FIRST:B,R2:A:SECOND:C,R3:B:TO_D:D,R4:C:TO_D:D"
                ),
                "A",
                "D"
        );

        assertEquals(
                java.util.List.of("A", "B", "D"),
                path.nodes().stream().map(TraceNode::id).toList()
        );
    }

    @Test
    void shouldNotModifySourceModel() throws Exception {
        JsonNode model = model("A,B", "R1:A:LINKS:B");
        JsonNode original = model.deepCopy();

        traceEngine.trace(model, "A", "B");

        assertEquals(original, model);
    }

    private JsonNode model(String nodeIds, String relationships)
            throws Exception {
        var root = objectMapper.createObjectNode();
        var nodes = root.putArray("nodes");

        if (!nodeIds.isBlank()) {
            for (String nodeId : nodeIds.split(",")) {
                nodes.addObject()
                        .put("id", nodeId)
                        .put("type", "TEST_NODE")
                        .put("name", "Node " + nodeId);
            }
        }

        var relationshipArray = root.putArray("relationships");
        if (!relationships.isBlank()) {
            for (String encoded : relationships.split(",")) {
                String[] fields = encoded.split(":");
                relationshipArray.addObject()
                        .put("id", fields[0])
                        .put("from", fields[1])
                        .put("type", fields[2])
                        .put("to", fields[3]);
            }
        }

        return root;
    }
}
