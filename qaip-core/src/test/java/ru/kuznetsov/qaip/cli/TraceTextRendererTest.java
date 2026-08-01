package ru.kuznetsov.qaip.cli;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.trace.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TraceTextRendererTest {
    private final TraceTextRenderer renderer = new TraceTextRenderer();

    @Test
    void renders_exact_deterministic_order_direction_cycles_and_self_reference() {
        TraceResult trace = new TraceResult("B", List.of(
                new TraceNode("B", " RULE "), new TraceNode("A", "STORY"), new TraceNode("C", "TEST")),
                List.of(
                        new TraceRelationship("CA", "C", "A", "COVERS"),
                        new TraceRelationship("AB", "A", "B", "DERIVES"),
                        new TraceRelationship("SELF", "B", "B", "REF")));
        String expected = String.join(System.lineSeparator(),
                "Trace", "Project ID: P", "Start Node ID: B", "", "Nodes:",
                "B |  RULE ", "A | STORY", "C | TEST", "", "Relationships:",
                "CA | COVERS | C -> A", "AB | DERIVES | A -> B", "SELF | REF | B -> B");
        assertEquals(expected, renderer.renderFound("P", "B", trace));
        assertEquals(expected, renderer.renderFound("P", "B", trace));
        assertFalse(expected.contains("TraceNode["));
        assertTrue(expected.indexOf("Nodes:") < expected.indexOf("Relationships:"));
    }

    @Test
    void renders_none_for_zero_relationships() {
        TraceResult trace = new TraceResult("A", List.of(new TraceNode("A", "TYPE")), List.of());
        assertTrue(renderer.renderFound("P", "A", trace)
                .endsWith("Relationships:" + System.lineSeparator() + "(none)"));
    }
}
