package ru.kuznetsov.qaip.core.application.query.relationship;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.node.ProjectNodeLookup;
import ru.kuznetsov.qaip.core.domain.*;
import ru.kuznetsov.qaip.core.persistence.ProjectPersistenceException;
import ru.kuznetsov.qaip.core.persistence.read.ProjectReader;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class DefaultRelationshipsUseCaseTest {
    @Test
    void existing_project_and_node_return_mapped_direct_relationships_with_exact_ids() {
        Node node = node(" N ");
        Relationship incoming = relationship("IN", "A", " N ");
        Relationship outgoing = relationship("OUT", " N ", "B");
        Project project = project(" P ", List.of(node), List.of(incoming, outgoing));
        ReaderSpy reader = new ReaderSpy(Optional.of(project));

        RelationshipsFound found = assertInstanceOf(RelationshipsFound.class,
                useCase(reader).execute(" P ", " N "));

        assertEquals(" P ", found.projectId());
        assertEquals(" N ", found.nodeId());
        assertEquals(List.of(details(incoming)), found.relationships().incoming());
        assertEquals(List.of(details(outgoing)), found.relationships().outgoing());
        assertEquals(" P ", reader.id.get());
        assertEquals(1, reader.calls.get());
    }

    @Test
    void existing_node_with_no_relationships_is_successful() {
        Project project = project("P", List.of(node("N")), List.of());
        RelationshipsFound found = assertInstanceOf(RelationshipsFound.class,
                useCase(new ReaderSpy(Optional.of(project))).execute("P", "N"));
        assertEquals(new RelationshipDetailsResult(List.of(), List.of()), found.relationships());
    }

    @Test
    void project_and_node_absence_short_circuit_into_distinct_exact_results() {
        ReaderSpy missingProject = new ReaderSpy(Optional.empty());
        assertEquals(new RelationshipsProjectNotFound(" P "),
                useCase(missingProject).execute(" P ", " N "));
        assertEquals(1, missingProject.calls.get());

        ReaderSpy missingNode = new ReaderSpy(Optional.of(project(" P ", List.of(), List.of(
                relationship(null, "bad", "bad")))));
        assertEquals(new RelationshipsNodeNotFound(" P ", " N "),
                useCase(missingNode).execute(" P ", " N "));
        assertEquals(1, missingNode.calls.get());
    }

    @Test
    void constructor_and_both_inputs_are_validated_before_any_read() {
        ReaderSpy reader = new ReaderSpy(Optional.empty());
        assertThrows(NullPointerException.class, () -> new DefaultRelationshipsUseCase(null,
                new ProjectNodeLookup(), new ProjectRelationshipLookup(), new RelationshipDetailsMapper()));
        assertThrows(NullPointerException.class, () -> new DefaultRelationshipsUseCase(reader,
                null, new ProjectRelationshipLookup(), new RelationshipDetailsMapper()));
        assertThrows(NullPointerException.class, () -> new DefaultRelationshipsUseCase(reader,
                new ProjectNodeLookup(), null, new RelationshipDetailsMapper()));
        assertThrows(NullPointerException.class, () -> new DefaultRelationshipsUseCase(reader,
                new ProjectNodeLookup(), new ProjectRelationshipLookup(), null));

        RelationshipsUseCase useCase = useCase(reader);
        assertThrows(NullPointerException.class, () -> useCase.execute(null, "N"));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(" \t", "N"));
        assertThrows(NullPointerException.class, () -> useCase.execute("P", null));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute("P", ""));
        assertEquals(0, reader.calls.get());
    }

    @Test
    void failures_and_null_reader_result_propagate_without_typed_conversion() {
        ProjectPersistenceException persistence = new ProjectPersistenceException("offline");
        ProjectReader failedReader = id -> { throw persistence; };
        assertSame(persistence, assertThrows(ProjectPersistenceException.class,
                () -> useCase(failedReader).execute("P", "N")));
        assertThrows(NullPointerException.class, () -> useCase(id -> null).execute("P", "N"));

        Node invalidLookupNode = node(null);
        assertThrows(NullPointerException.class, () -> useCase(new ReaderSpy(Optional.of(
                project("P", List.of(invalidLookupNode), List.of())))).execute("P", "N"));

        Relationship invalidMappingRelationship = relationship(null, "N", "X");
        assertThrows(NullPointerException.class, () -> useCase(new ReaderSpy(Optional.of(
                project("P", List.of(node("N")), List.of(invalidMappingRelationship))))).execute("P", "N"));
    }

    static DefaultRelationshipsUseCase useCase(ProjectReader reader) {
        return new DefaultRelationshipsUseCase(reader, new ProjectNodeLookup(),
                new ProjectRelationshipLookup(), new RelationshipDetailsMapper());
    }

    static Node node(String id) {
        return new Node(id, "REQUIREMENT", "name", "description", "status",
                List.of(), List.of(), Map.of(), Map.of());
    }

    static Relationship relationship(String id, String from, String to) {
        return new Relationship(id, from, "DIRECT", to, Map.of(), List.of());
    }

    static RelationshipDetails details(Relationship relationship) {
        return new RelationshipDetails(relationship.id(), relationship.from(),
                relationship.to(), relationship.type());
    }

    static Project project(String id, List<Node> nodes, List<Relationship> relationships) {
        return new Project("contract", "schema", new Metadata(id, "project", null, null, Map.of()), List.of(),
                new Subject("local"), nodes, relationships, new EvidenceManifest("evidence", "source", Map.of(),
                "normalization", "canonicalization", "fingerprint", List.of(), List.of(), List.of()),
                List.of(), Map.of());
    }

    private static final class ReaderSpy implements ProjectReader {
        private final Optional<Project> result;
        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicReference<String> id = new AtomicReference<>();

        private ReaderSpy(Optional<Project> result) {
            this.result = result;
        }

        @Override
        public Optional<Project> findById(String projectId) {
            calls.incrementAndGet();
            id.set(projectId);
            return result;
        }
    }
}
