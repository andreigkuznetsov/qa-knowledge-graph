package ru.kuznetsov.qaip.core.application.query.nodedetails;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NodeDetailsResultTest {
    @Test
    void is_a_five_scalar_component_value_preserving_exact_text_and_optional_nulls() {
        NodeDetailsResult first = new NodeDetailsResult(" N ", " Type ", " Name ", null, null);
        NodeDetailsResult equal = new NodeDetailsResult(" N ", " Type ", " Name ", null, null);
        assertEquals(first, equal);
        assertEquals(first.hashCode(), equal.hashCode());
        assertEquals(" N ", first.nodeId());
        assertEquals(" Type ", first.nodeType());
        assertEquals(" Name ", first.name());
        assertNull(first.description());
        assertNull(first.status());
        assertDoesNotThrow(() -> new NodeDetailsResult("N", "T", "name", "", ""));
    }

    @Test
    void rejects_null_or_blank_mandatory_fields_independently() {
        assertThrows(NullPointerException.class, () -> result(null, "T", "name"));
        assertThrows(IllegalArgumentException.class, () -> result(" ", "T", "name"));
        assertThrows(NullPointerException.class, () -> result("N", null, "name"));
        assertThrows(IllegalArgumentException.class, () -> result("N", "\t", "name"));
        assertThrows(NullPointerException.class, () -> result("N", "T", null));
        assertThrows(IllegalArgumentException.class, () -> result("N", "T", "\n"));
    }

    private static NodeDetailsResult result(String id, String type, String name) {
        return new NodeDetailsResult(id, type, name, null, null);
    }
}
