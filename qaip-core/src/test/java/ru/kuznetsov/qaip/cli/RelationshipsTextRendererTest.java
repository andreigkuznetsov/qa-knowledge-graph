package ru.kuznetsov.qaip.cli;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.relationship.RelationshipDetails;
import ru.kuznetsov.qaip.core.application.query.relationship.RelationshipDetailsResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RelationshipsTextRendererTest {
    private final RelationshipsTextRenderer renderer = new RelationshipsTextRenderer();

    @Test
    void renders_exact_sections_order_duplicates_and_self_reference_deterministically() {
        RelationshipDetails incoming = details("IN", "A", "N", "DERIVES_FROM");
        RelationshipDetails self = details("SELF", "N", "N", "RELATED_TO");
        RelationshipDetails outgoing = details("OUT", "N", "B", "IMPLEMENTS");
        var result = new RelationshipDetailsResult(List.of(incoming, self), List.of(outgoing, self, outgoing));
        String expected = String.join(System.lineSeparator(),
                "Relationships", "Project ID:  P ", "Node ID:  N ", "", "Incoming:",
                "IN | DERIVES_FROM | A -> N", "SELF | RELATED_TO | N -> N", "", "Outgoing:",
                "OUT | IMPLEMENTS | N -> B", "SELF | RELATED_TO | N -> N", "OUT | IMPLEMENTS | N -> B");
        assertEquals(expected, renderer.renderFound(" P ", " N ", result));
        assertEquals(expected, renderer.renderFound(" P ", " N ", result));
        assertFalse(expected.contains(incoming.toString()));
    }

    @Test
    void renders_none_for_one_or_both_empty_directions() {
        RelationshipDetails outgoing = details("OUT", "N", "B", "DIRECT");
        assertEquals(String.join(System.lineSeparator(), "Relationships", "Project ID: P", "Node ID: N", "",
                        "Incoming:", "(none)", "", "Outgoing:", "OUT | DIRECT | N -> B"),
                renderer.renderFound("P", "N", new RelationshipDetailsResult(List.of(), List.of(outgoing))));
        String both = renderer.renderFound("P", "N", new RelationshipDetailsResult(List.of(), List.of()));
        assertEquals(2, occurrences(both, "(none)"));
    }

    private static RelationshipDetails details(String id, String from, String to, String type) {
        return new RelationshipDetails(id, from, to, type);
    }

    private static int occurrences(String source, String token) {
        return (source.length() - source.replace(token, "").length()) / token.length();
    }
}
