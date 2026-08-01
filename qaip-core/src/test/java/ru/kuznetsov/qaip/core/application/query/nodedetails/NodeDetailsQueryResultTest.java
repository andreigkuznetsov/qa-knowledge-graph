package ru.kuznetsov.qaip.core.application.query.nodedetails;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class NodeDetailsQueryResultTest {
    @Test
    void found_preserves_exact_details_and_has_value_semantics() {
        NodeDetailsResult details = details();
        NodeDetailsFound first = new NodeDetailsFound(details);
        NodeDetailsFound equal = new NodeDetailsFound(details);
        assertSame(details, first.details());
        assertEquals(first, equal);
        assertEquals(first.hashCode(), equal.hashCode());
        assertThrows(NullPointerException.class, () -> new NodeDetailsFound(null));
        assertEquals(1, NodeDetailsFound.class.getRecordComponents().length);
        assertEquals(NodeDetailsResult.class, NodeDetailsFound.class.getRecordComponents()[0].getType());
    }

    @Test
    void project_not_found_validates_and_preserves_exact_id() {
        NodeDetailsProjectNotFound first = new NodeDetailsProjectNotFound(" P ");
        assertEquals(" P ", first.projectId());
        assertEquals(first, new NodeDetailsProjectNotFound(" P "));
        assertEquals(first.hashCode(), new NodeDetailsProjectNotFound(" P ").hashCode());
        assertThrows(NullPointerException.class, () -> new NodeDetailsProjectNotFound(null));
        assertThrows(IllegalArgumentException.class, () -> new NodeDetailsProjectNotFound(" \t"));
        assertEquals(1, NodeDetailsProjectNotFound.class.getRecordComponents().length);
    }

    @Test
    void node_not_found_validates_and_preserves_both_exact_ids() {
        NodeDetailsNodeNotFound first = new NodeDetailsNodeNotFound(" P ", " N ");
        assertEquals(" P ", first.projectId());
        assertEquals(" N ", first.nodeId());
        assertEquals(first, new NodeDetailsNodeNotFound(" P ", " N "));
        assertEquals(first.hashCode(), new NodeDetailsNodeNotFound(" P ", " N ").hashCode());
        assertThrows(NullPointerException.class, () -> new NodeDetailsNodeNotFound(null, "N"));
        assertThrows(IllegalArgumentException.class, () -> new NodeDetailsNodeNotFound(" ", "N"));
        assertThrows(NullPointerException.class, () -> new NodeDetailsNodeNotFound("P", null));
        assertThrows(IllegalArgumentException.class, () -> new NodeDetailsNodeNotFound("P", "\n"));
        assertEquals(2, NodeDetailsNodeNotFound.class.getRecordComponents().length);
    }

    @Test
    void hierarchy_is_sealed_with_exactly_three_expected_outcomes() {
        assertTrue(NodeDetailsQueryResult.class.isSealed());
        assertEquals(Set.of(NodeDetailsFound.class, NodeDetailsProjectNotFound.class,
                        NodeDetailsNodeNotFound.class),
                java.util.Arrays.stream(NodeDetailsQueryResult.class.getPermittedSubclasses())
                        .collect(Collectors.toSet()));
    }

    private static NodeDetailsResult details() {
        return new NodeDetailsResult("N", "CHECK", "name", null, null);
    }
}
