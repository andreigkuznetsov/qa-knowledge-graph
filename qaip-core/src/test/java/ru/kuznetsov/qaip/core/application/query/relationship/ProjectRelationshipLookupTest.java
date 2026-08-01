package ru.kuznetsov.qaip.core.application.query.relationship;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.domain.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProjectRelationshipLookupTest {
    private final ProjectRelationshipLookup lookup = new ProjectRelationshipLookup();

    @Test
    void separates_direct_direction_excludes_unrelated_and_includes_self_reference_twice() {
        Relationship incoming = relationship("IN", "A", "N");
        Relationship unrelated = relationship("OTHER", "A", "B");
        Relationship outgoing = relationship("OUT", "N", "B");
        Relationship self = relationship("SELF", "N", "N");
        ProjectRelationships result = lookup.findByNodeId(
                project(List.of(incoming, unrelated, outgoing, self)), "N");

        assertEquals(List.of(incoming, self), result.incoming());
        assertEquals(List.of(outgoing, self), result.outgoing());
        assertSame(incoming, result.incoming().getFirst());
        assertSame(outgoing, result.outgoing().getFirst());
        assertSame(self, result.incoming().getLast());
        assertSame(self, result.outgoing().getLast());
    }

    @Test
    void preserves_independent_project_order_duplicates_and_exact_instances() {
        Relationship out1 = relationship("R-1", "N", "A");
        Relationship in1 = relationship("R-2", "A", "N");
        Relationship out2 = relationship("R-3", "N", "B");
        Relationship in2 = relationship("R-4", "B", "N");
        Project project = project(List.of(out1, in1, out2, out1, in2));
        Project before = project;

        ProjectRelationships first = lookup.findByNodeId(project, "N");
        assertEquals(List.of(in1, in2), first.incoming());
        assertEquals(List.of(out1, out2, out1), first.outgoing());
        assertSame(out1, first.outgoing().getFirst());
        assertSame(out1, first.outgoing().getLast());
        assertEquals(first, lookup.findByNodeId(project, "N"));
        assertEquals(before, project);
    }

    @Test
    void matching_is_exact_case_and_whitespace_sensitive_without_requiring_a_node() {
        Relationship plain = relationship("PLAIN", "REQ-17", "X");
        Relationship spaced = relationship("SPACED", "X", " REQ-17 ");
        Project project = project(List.of(plain, spaced));
        assertEquals(List.of(), lookup.findByNodeId(project, "req-17").outgoing());
        assertEquals(List.of(), lookup.findByNodeId(project, "REQ-17 ").outgoing());
        assertEquals(List.of(plain), lookup.findByNodeId(project, "REQ-17").outgoing());
        assertEquals(List.of(spaced), lookup.findByNodeId(project, " REQ-17 ").incoming());
        assertTrue(project.nodes().isEmpty());
    }

    @Test
    void no_matches_and_empty_relationship_collection_return_two_empty_lists() {
        ProjectRelationships missing = lookup.findByNodeId(
                project(List.of(relationship("R", "A", "B"))), "N");
        assertTrue(missing.incoming().isEmpty());
        assertTrue(missing.outgoing().isEmpty());
        assertEquals(new ProjectRelationships(List.of(), List.of()), lookup.findByNodeId(project(List.of()), "N"));
    }

    @Test
    void validates_inputs_and_result_defensively_copies_non_null_elements() {
        Project project = project(List.of());
        assertThrows(NullPointerException.class, () -> lookup.findByNodeId(null, "N"));
        assertThrows(NullPointerException.class, () -> lookup.findByNodeId(project, null));
        assertThrows(IllegalArgumentException.class, () -> lookup.findByNodeId(project, ""));
        assertThrows(IllegalArgumentException.class, () -> lookup.findByNodeId(project, " \t"));
        assertThrows(NullPointerException.class, () -> new ProjectRelationships(null, List.of()));
        assertThrows(NullPointerException.class, () -> new ProjectRelationships(List.of(), null));
        List<Relationship> withNull = new ArrayList<>();
        withNull.add(null);
        assertThrows(NullPointerException.class, () -> new ProjectRelationships(withNull, List.of()));

        List<Relationship> source = new ArrayList<>();
        source.add(relationship("R", "A", "B"));
        ProjectRelationships result = new ProjectRelationships(source, List.of());
        source.clear();
        assertEquals(1, result.incoming().size());
        assertThrows(UnsupportedOperationException.class, () -> result.incoming().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.outgoing().add(sourceRelationship()));
    }

    private static Relationship sourceRelationship() {
        return relationship("X", "A", "B");
    }
    private static Relationship relationship(String id, String from, String to) {
        return new Relationship(id, from, "DIRECT", to, Map.of(), List.of());
    }
    private static Project project(List<Relationship> relationships) {
        return new Project("contract", "schema", new Metadata("P", "Project", null, null, Map.of()),
                List.of(), new Subject("local"), List.of(), relationships, new EvidenceManifest("evidence",
                "source", Map.of(), "normalization", "canonicalization", "fingerprint",
                List.of(), List.of(), List.of()), List.of(), Map.of());
    }
}
