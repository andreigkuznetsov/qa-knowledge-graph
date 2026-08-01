package ru.kuznetsov.qaip.core.application.query.relationship;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RelationshipDetailsResultTest {
    @Test
    void scalar_details_preserve_exact_text_and_have_record_semantics() {
        RelationshipDetails first = details(" ID ", " From ", " To ", " Type ");
        RelationshipDetails equal = details(" ID ", " From ", " To ", " Type ");
        assertEquals(first, equal);
        assertEquals(first.hashCode(), equal.hashCode());
        assertEquals(" ID ", first.relationshipId());
        assertEquals(" From ", first.fromNodeId());
        assertEquals(" To ", first.toNodeId());
        assertEquals(" Type ", first.relationshipType());
    }

    @Test
    void rejects_each_null_or_blank_mandatory_scalar() {
        assertInvalid(null, "F", "T", "Y"); assertInvalid(" ", "F", "T", "Y");
        assertInvalid("I", null, "T", "Y"); assertInvalid("I", "\t", "T", "Y");
        assertInvalid("I", "F", null, "Y"); assertInvalid("I", "F", "\n", "Y");
        assertInvalid("I", "F", "T", null); assertInvalid("I", "F", "T", " ");
    }

    @Test
    void aggregate_defensively_copies_rejects_nulls_and_exposes_immutable_lists() {
        RelationshipDetails details = details("I", "F", "T", "Y");
        List<RelationshipDetails> incoming = new ArrayList<>(List.of(details));
        RelationshipDetailsResult result = new RelationshipDetailsResult(incoming, List.of());
        incoming.clear();
        assertEquals(List.of(details), result.incoming());
        assertThrows(UnsupportedOperationException.class, () -> result.incoming().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.outgoing().add(details));
        assertThrows(NullPointerException.class, () -> new RelationshipDetailsResult(null, List.of()));
        assertThrows(NullPointerException.class, () -> new RelationshipDetailsResult(List.of(), null));
        List<RelationshipDetails> withNull = new ArrayList<>(); withNull.add(null);
        assertThrows(NullPointerException.class, () -> new RelationshipDetailsResult(withNull, List.of()));
    }

    private static void assertInvalid(String id, String from, String to, String type) {
        assertThrows(RuntimeException.class, () -> details(id, from, to, type));
    }
    private static RelationshipDetails details(String id, String from, String to, String type) {
        return new RelationshipDetails(id, from, to, type);
    }
}
