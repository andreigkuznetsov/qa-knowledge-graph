package ru.kuznetsov.qaip.core.application.query.node;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.domain.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProjectNodeLookupTest {
    private final ProjectNodeLookup lookup = new ProjectNodeLookup();

    @Test
    void returns_exact_first_middle_and_last_instances_without_type_filtering() {
        Node first = node("N-1", "REQUIREMENT", "first");
        Node middle = node("N-2", "SCENARIO", "middle");
        Node last = node("N-3", "ACTOR", "last");
        Project project = project(List.of(first, middle, last));

        assertSame(first, lookup.findById(project, "N-1").orElseThrow());
        assertSame(middle, lookup.findById(project, "N-2").orElseThrow());
        assertSame(last, lookup.findById(project, "N-3").orElseThrow());
        for (String type : List.of("REQUIREMENT", "SCENARIO", "BUSINESS_RULE", "TEST", "ACTOR", "CUSTOM")) {
            Node typed = node("ID-" + type, type, type);
            assertSame(typed, lookup.findById(project(List.of(typed)), typed.id()).orElseThrow());
        }
    }

    @Test
    void missing_and_empty_projects_return_empty() {
        assertTrue(lookup.findById(project(List.of(node("N-1", "TEST", "node"))), "missing").isEmpty());
        assertTrue(lookup.findById(project(List.of()), "missing").isEmpty());
    }

    @Test
    void validates_inputs_without_mutating_project() {
        Project project = project(List.of(node("N-1", "TEST", "node")));
        Project before = project;
        List<Node> order = project.nodes();
        assertThrows(NullPointerException.class, () -> lookup.findById(null, "N-1"));
        assertThrows(NullPointerException.class, () -> lookup.findById(project, null));
        assertThrows(IllegalArgumentException.class, () -> lookup.findById(project, ""));
        assertThrows(IllegalArgumentException.class, () -> lookup.findById(project, " \t"));
        assertEquals(before, project);
        assertEquals(order, project.nodes());
    }

    @Test
    void matching_is_exact_case_and_whitespace_sensitive() {
        Node plain = node("REQ-17", "REQUIREMENT", "plain");
        Node spaced = node(" REQ-17 ", "TEST", "spaced");
        Project project = project(List.of(plain, spaced));
        assertTrue(lookup.findById(project, "req-17").isEmpty());
        assertTrue(lookup.findById(project, "REQ-17 ").isEmpty());
        assertTrue(lookup.findById(project, " REQ-17").isEmpty());
        assertSame(plain, lookup.findById(project, "REQ-17").orElseThrow());
        assertSame(spaced, lookup.findById(project, " REQ-17 ").orElseThrow());
    }

    @Test
    void directly_constructed_duplicates_return_first_match_deterministically() {
        Node first = node("DUP", "TEST", "first");
        Node second = node("DUP", "REQUIREMENT", "second");
        Project project = project(List.of(first, second));
        assertSame(first, lookup.findById(project, "DUP").orElseThrow());
        assertSame(first, lookup.findById(project, "DUP").orElseThrow());
        assertEquals(List.of(first, second), project.nodes());
    }

    private static Node node(String id, String type, String name) {
        return new Node(id, type, name, null, null, List.of(), List.of(), Map.of(), Map.of());
    }

    private static Project project(List<Node> nodes) {
        return new Project("contract", "schema", new Metadata("P", "project", null, null, Map.of()),
                List.of(), new Subject("local"), nodes, List.of(), new EvidenceManifest("evidence", "source",
                Map.of(), "normalization", "canonicalization", "fingerprint", List.of(), List.of(), List.of()),
                List.of(), Map.of());
    }
}
