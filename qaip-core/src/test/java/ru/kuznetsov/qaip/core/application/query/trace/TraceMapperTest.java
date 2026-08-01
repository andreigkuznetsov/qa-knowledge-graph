package ru.kuznetsov.qaip.core.application.query.trace;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.domain.Node;
import ru.kuznetsov.qaip.core.domain.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TraceMapperTest {
    private final TraceMapper mapper = new TraceMapper();

    @Test
    void maps_one_node_with_exact_scalars() {
        TraceResult result = mapper.map(new TraceGraph(" N-1 ",
                List.of(node(" N-1 ", " story ")), List.of()));
        assertEquals(" N-1 ", result.startNodeId());
        assertEquals(List.of(new TraceNode(" N-1 ", " story ")), result.nodes());
        assertTrue(result.relationships().isEmpty());
    }

    @Test
    void preserves_node_and_relationship_order_and_exact_values() {
        Node b = node("B", "Business Rule");
        Node a = node("A", "User Story");
        Node c = node("C", "Test Case");
        Relationship secondById = relationship("R-2", "A", "C", " verifies ");
        Relationship firstById = relationship("R-1", "B", "A", "derives");
        TraceGraph graph = new TraceGraph("B", List.of(b, a, c), List.of(secondById, firstById));

        TraceResult result = mapper.map(graph);

        assertEquals(List.of(
                new TraceNode("B", "Business Rule"),
                new TraceNode("A", "User Story"),
                new TraceNode("C", "Test Case")), result.nodes());
        assertEquals(List.of(
                new TraceRelationship("R-2", "A", "C", " verifies "),
                new TraceRelationship("R-1", "B", "A", "derives")), result.relationships());
        assertEquals(result, mapper.map(graph));
    }

    @Test
    void preserves_duplicate_scalar_values_without_deduplication() {
        TraceResult result = mapper.map(new TraceGraph("A",
                List.of(node("A", "same"), node("B", "same")),
                List.of(
                        relationship("R1", "A", "B", "same"),
                        relationship("R2", "A", "B", "same"))));
        assertEquals(List.of("same", "same"), result.nodes().stream().map(TraceNode::nodeType).toList());
        assertEquals(List.of("same", "same"), result.relationships().stream()
                .map(TraceRelationship::relationshipType).toList());
        assertEquals(List.of("A", "A"), result.relationships().stream()
                .map(TraceRelationship::fromNodeId).toList());
    }

    @Test
    void rejects_null_mapper_and_dto_inputs_and_blank_ids() {
        assertThrows(NullPointerException.class, () -> mapper.map(null));
        assertThrows(NullPointerException.class, () -> new TraceNode(null, "type"));
        assertThrows(IllegalArgumentException.class, () -> new TraceNode(" ", "type"));
        assertThrows(NullPointerException.class, () -> new TraceNode("A", null));
        assertThrows(NullPointerException.class, () -> new TraceRelationship(null, "A", "B", "type"));
        assertThrows(IllegalArgumentException.class, () -> new TraceRelationship("R", "", "B", "type"));
        assertThrows(IllegalArgumentException.class, () -> new TraceRelationship("R", "A", "\t", "type"));
        assertThrows(NullPointerException.class, () -> new TraceRelationship("R", "A", "B", null));
        assertThrows(NullPointerException.class, () -> new TraceResult(null, List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new TraceResult(" ", List.of(), List.of()));
        assertThrows(NullPointerException.class, () -> new TraceResult("A", null, List.of()));
        assertThrows(NullPointerException.class, () -> new TraceResult("A", List.of(), null));
        assertThrows(NullPointerException.class, () -> new TraceResult("A", nodesWithNull(), List.of()));
        assertThrows(NullPointerException.class, () -> new TraceResult("A", List.of(), relationshipsWithNull()));
    }

    @Test
    void result_defensively_copies_and_exposes_immutable_lists() {
        var nodes = new ArrayList<>(List.of(new TraceNode("A", "type")));
        var relationships = new ArrayList<>(List.of(new TraceRelationship("R", "A", "A", "self")));
        TraceResult result = new TraceResult("A", nodes, relationships);
        nodes.clear();
        relationships.clear();
        assertEquals(1, result.nodes().size());
        assertEquals(1, result.relationships().size());
        assertThrows(UnsupportedOperationException.class, () -> result.nodes().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.relationships().clear());
    }

    private static List<TraceNode> nodesWithNull() {
        var values = new ArrayList<TraceNode>();
        values.add(null);
        return values;
    }

    private static List<TraceRelationship> relationshipsWithNull() {
        var values = new ArrayList<TraceRelationship>();
        values.add(null);
        return values;
    }

    private static Node node(String id, String type) {
        return new Node(id, type, "name", null, null, List.of(), List.of(), Map.of(), Map.of());
    }

    private static Relationship relationship(String id, String from, String to, String type) {
        return new Relationship(id, from, type, to, Map.of(), List.of());
    }
}
