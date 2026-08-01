package ru.kuznetsov.qaip.core.application.query.trace;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.domain.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TraceGraphBuilderTest {
    private final TraceGraphBuilder builder = new TraceGraphBuilder();

    @Test
    void includes_isolated_start_and_excludes_disconnected_content() {
        Node start = node("START");
        Project project = project(List.of(start, node("X"), node("Y")), List.of(relationship("R-X", "X", "Y")));
        TraceGraph result = builder.build(project, "START");
        assertEquals(List.of(start), result.nodes());
        assertTrue(result.relationships().isEmpty());
        assertSame(start, result.nodes().getFirst());
    }

    @Test
    void traverses_incoming_and_outgoing_transitively_in_breadth_first_order() {
        Node us = node("US");
        Node br = node("BR");
        Node sc1 = node("SC-1");
        Node sc2 = node("SC-2");
        Node test = node("TEST");
        Relationship toTest = relationship("R-4", "SC-1", "TEST");
        Relationship toSc2 = relationship("R-3", "BR", "SC-2");
        Relationship upstream = relationship("R-1", "US", "BR");
        Relationship toSc1 = relationship("R-2", "BR", "SC-1");
        Project project = project(List.of(us, br, sc1, sc2, test),
                List.of(toTest, toSc2, upstream, toSc1));

        TraceGraph result = builder.build(project, "BR");

        assertEquals(List.of(br, sc2, us, sc1, test), result.nodes());
        assertEquals(List.of(toTest, toSc2, upstream, toSc1), result.relationships());
        assertSame(upstream, result.relationships().get(2));
        assertSame(test, result.nodes().getLast());
        assertEquals(result, builder.build(project, "BR"));
    }

    @Test
    void traverses_through_incoming_then_outgoing_and_returns_complete_component() {
        Node a = node("A");
        Node b = node("B");
        Node c = node("C");
        Node d = node("D");
        Relationship ab = relationship("AB", "A", "B");
        Relationship ac = relationship("AC", "A", "C");
        Relationship cd = relationship("CD", "C", "D");
        TraceGraph result = builder.build(project(List.of(a, b, c, d), List.of(ab, ac, cd)), "B");
        assertEquals(List.of(b, a, c, d), result.nodes());
        assertEquals(List.of(ab, ac, cd), result.relationships());
    }

    @Test
    void cycle_self_reference_and_multiple_paths_are_cycle_safe_and_not_duplicated() {
        Node a = node("A");
        Node b = node("B");
        Node c = node("C");
        Relationship ab = relationship("AB", "A", "B");
        Relationship bc = relationship("BC", "B", "C");
        Relationship ca = relationship("CA", "C", "A");
        Relationship self = relationship("SELF", "A", "A");
        Relationship ac = relationship("AC", "A", "C");
        TraceGraph result = builder.build(project(List.of(a, b, c), List.of(ab, bc, ca, self, ac)), "A");
        assertEquals(List.of(a, b, c), result.nodes());
        assertEquals(List.of(ab, bc, ca, self, ac), result.relationships());
        assertSame(self, result.relationships().get(3));
    }

    @Test
    void validates_builder_inputs_missing_start_and_reachable_dangling_endpoint() {
        Project project = project(List.of(node("A")), List.of());
        assertThrows(NullPointerException.class, () -> builder.build(null, "A"));
        assertThrows(NullPointerException.class, () -> builder.build(project, null));
        assertThrows(IllegalArgumentException.class, () -> builder.build(project, ""));
        assertThrows(IllegalArgumentException.class, () -> builder.build(project, " \t"));
        IllegalArgumentException missing = assertThrows(IllegalArgumentException.class,
                () -> builder.build(project, "MISSING"));
        assertTrue(missing.getMessage().contains("MISSING"));
        IllegalStateException dangling = assertThrows(IllegalStateException.class, () -> builder.build(
                project(List.of(node("A")), List.of(relationship("R", "A", "ABSENT"))), "A"));
        assertTrue(dangling.getMessage().contains("ABSENT"));
    }

    @Test
    void preserves_exact_id_and_does_not_modify_project() {
        Node exact = node(" A ");
        Project project = project(List.of(exact), List.of());
        List<Node> beforeNodes = project.nodes();
        assertEquals(" A ", builder.build(project, " A ").startNodeId());
        assertThrows(IllegalArgumentException.class, () -> builder.build(project, "A"));
        assertSame(beforeNodes, project.nodes());
    }

    @Test
    void trace_graph_is_defensive_immutable_and_enforces_identity_contracts() {
        Node a = node("A");
        Relationship r = relationship("R", "A", "A");
        var nodes = new ArrayList<>(List.of(a));
        var relationships = new ArrayList<>(List.of(r));
        TraceGraph graph = new TraceGraph("A", nodes, relationships);
        nodes.clear();
        relationships.clear();
        assertEquals(List.of(a), graph.nodes());
        assertThrows(UnsupportedOperationException.class, () -> graph.nodes().clear());
        assertThrows(UnsupportedOperationException.class, () -> graph.relationships().clear());
        assertThrows(NullPointerException.class, () -> new TraceGraph(null, List.of(a), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new TraceGraph(" ", List.of(a), List.of()));
        assertThrows(NullPointerException.class, () -> new TraceGraph("A", null, List.of()));
        assertThrows(NullPointerException.class, () -> new TraceGraph("A", List.of(a), null));
        assertThrows(NullPointerException.class, () -> new TraceGraph("A", listWithNull(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new TraceGraph("A", List.of(node("B")), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new TraceGraph("A", List.of(a, node("A")), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new TraceGraph("A", List.of(a),
                List.of(r, relationship("R", "A", "A"))));
    }

    private static List<Node> listWithNull() {
        var result = new ArrayList<Node>();
        result.add(null);
        return result;
    }

    private static Node node(String id) {
        return new Node(id, "type", id, null, null, List.of(), List.of(), Map.of(), Map.of());
    }

    private static Relationship relationship(String id, String from, String to) {
        return new Relationship(id, from, "DIRECT", to, Map.of(), List.of());
    }

    private static Project project(List<Node> nodes, List<Relationship> relationships) {
        return new Project("contract", "schema", new Metadata("P", "Project", null, null, Map.of()),
                List.of(), new Subject("local"), nodes, relationships, new EvidenceManifest("evidence",
                "source", Map.of(), "normalization", "canonicalization", "fingerprint",
                List.of(), List.of(), List.of()), List.of(), Map.of());
    }
}
