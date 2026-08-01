package ru.kuznetsov.qaip.core.application.query.nodedetails;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.kuznetsov.qaip.core.domain.Node;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NodeDetailsMapperTest {
    private final NodeDetailsMapper mapper = new NodeDetailsMapper();

    @Test
    void maps_exact_fixed_fields_deterministically_and_preserves_optional_nulls() {
        Node complete = node(" N-Id ", " Custom_Type ", " Name ", " Description ", " Mixed_Status ",
                Map.of("ignored", "value"));
        NodeDetailsResult expected = new NodeDetailsResult(
                " N-Id ", " Custom_Type ", " Name ", " Description ", " Mixed_Status ");
        assertEquals(expected, mapper.map(complete));
        assertEquals(expected, mapper.map(complete));

        assertEquals(new NodeDetailsResult("N", "CHECK", "name", null, null),
                mapper.map(node("N", "CHECK", "name", null, null, Map.of())));
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER_STORY", "BUSINESS_RULE", "SCENARIO", "CHECK",
            "BUSINESS_OPERATION", "TECHNICAL_IMPLEMENTATION", "TEST_IMPLEMENTATION", "CUSTOM"})
    void accepts_every_type_without_interpretation(String type) {
        assertEquals(type, mapper.map(node("N", type, "name", null, null, Map.of())).nodeType());
    }

    @Test
    void fixed_fields_are_authoritative_and_attributes_never_affect_result() {
        Map<String, Object> conflicting = Map.of(
                "name", "attribute name",
                "description", "attribute description",
                "status", "attribute status",
                "nested", Map.of("list", List.of("a", "b")));
        Node first = node("N", "SCENARIO", "fixed name", "fixed description", "fixed status", conflicting);
        Node second = node("N", "SCENARIO", "fixed name", "fixed description", "fixed status",
                Map.of("entirely", "different"));
        Node before = first;
        Map<String, Object> attributesBefore = first.attributes();

        NodeDetailsResult expected = new NodeDetailsResult(
                "N", "SCENARIO", "fixed name", "fixed description", "fixed status");
        assertEquals(expected, mapper.map(first));
        assertEquals(expected, mapper.map(second));
        assertEquals(before, first);
        assertEquals(attributesBefore, first.attributes());
        assertEquals(Map.of("list", List.of("a", "b")), first.attributes().get("nested"));
    }

    @Test
    void rejects_null_input() {
        assertThrows(NullPointerException.class, () -> mapper.map(null));
    }

    private static Node node(String id, String type, String name, String description, String status,
                             Map<String, Object> attributes) {
        return new Node(id, type, name, description, status, List.of("tag"),
                List.of(Map.of("source", "ignored")), Map.of("metadata", "ignored"), attributes);
    }
}
