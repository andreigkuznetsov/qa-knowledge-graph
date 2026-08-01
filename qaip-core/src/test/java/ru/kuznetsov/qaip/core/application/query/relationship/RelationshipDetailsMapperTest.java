package ru.kuznetsov.qaip.core.application.query.relationship;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.domain.Relationship;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RelationshipDetailsMapperTest {
    private final RelationshipDetailsMapper mapper = new RelationshipDetailsMapper();

    @Test
    void maps_exact_fixed_scalars_and_preserves_direction_order_and_duplicates() {
        Relationship incoming1 = relationship(" In-1 ", " From-A ", " Type_A ", " Node ");
        Relationship incoming2 = relationship("In-2", "From-B", "Type_B", "Node");
        Relationship outgoing1 = relationship("Out-1", "Node", "Type_C", "To-A");
        ProjectRelationships source = new ProjectRelationships(
                List.of(incoming1, incoming2, incoming1), List.of(outgoing1, outgoing1));

        RelationshipDetailsResult result = mapper.map(source);
        RelationshipDetails mappedIncoming = new RelationshipDetails(
                " In-1 ", " From-A ", " Node ", " Type_A ");
        RelationshipDetails mappedOutgoing = new RelationshipDetails(
                "Out-1", "Node", "To-A", "Type_C");
        assertEquals(List.of(mappedIncoming,
                new RelationshipDetails("In-2", "From-B", "Node", "Type_B"), mappedIncoming), result.incoming());
        assertEquals(List.of(mappedOutgoing, mappedOutgoing), result.outgoing());
        assertEquals(source, new ProjectRelationships(
                List.of(incoming1, incoming2, incoming1), List.of(outgoing1, outgoing1)));
        assertEquals(result, mapper.map(source));
    }

    @Test
    void preserves_preclassified_self_reference_once_in_each_direction_without_recalculation() {
        Relationship self = relationship("SELF", "N", "SELF_TYPE", "N");
        Relationship incomingOnlyByMembership = relationship("MEMBER", "N", "TYPE", "X");
        RelationshipDetailsResult result = mapper.map(
                new ProjectRelationships(List.of(self, incomingOnlyByMembership), List.of(self)));
        assertEquals(List.of("SELF", "MEMBER"), result.incoming().stream()
                .map(RelationshipDetails::relationshipId).toList());
        assertEquals(List.of("SELF"), result.outgoing().stream()
                .map(RelationshipDetails::relationshipId).toList());
    }

    @Test
    void maps_empty_lists_and_rejects_null_input() {
        assertEquals(new RelationshipDetailsResult(List.of(), List.of()),
                mapper.map(new ProjectRelationships(List.of(), List.of())));
        assertThrows(NullPointerException.class, () -> mapper.map(null));
    }

    private static Relationship relationship(String id, String from, String type, String to) {
        return new Relationship(id, from, type, to, Map.of("ignored", List.of(1, 2)),
                List.of(Map.of("ignored", true)));
    }
}
