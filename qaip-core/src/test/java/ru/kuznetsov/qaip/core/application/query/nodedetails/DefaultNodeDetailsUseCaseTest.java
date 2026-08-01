package ru.kuznetsov.qaip.core.application.query.nodedetails;

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

class DefaultNodeDetailsUseCaseTest {
    @Test
    void existing_project_and_node_return_details_with_one_exact_read() {
        Node node = node(" N ", "CHECK", "name");
        Project project = project(" P ", List.of(node));
        ReaderSpy reader = new ReaderSpy(Optional.of(project));
        var useCase = useCase(reader);
        NodeDetailsResult expected = new NodeDetailsMapper().map(node);

        NodeDetailsFound found = assertInstanceOf(NodeDetailsFound.class, useCase.execute(" P ", " N "));
        assertEquals(expected, found.details());
        assertEquals(" P ", reader.id.get());
        assertEquals(1, reader.calls.get());
        assertEquals(found, useCase.execute(" P ", " N "));
        assertEquals(2, reader.calls.get());
    }

    @Test
    void project_and_node_absence_are_distinct_exact_typed_outcomes() {
        ReaderSpy absent = new ReaderSpy(Optional.empty());
        assertEquals(new NodeDetailsProjectNotFound(" P "), useCase(absent).execute(" P ", " N "));
        assertEquals(1, absent.calls.get());

        ReaderSpy present = new ReaderSpy(Optional.of(project(" P ", List.of())));
        assertEquals(new NodeDetailsNodeNotFound(" P ", " N "), useCase(present).execute(" P ", " N "));
        assertEquals(1, present.calls.get());
    }

    @Test
    void dependencies_and_both_inputs_are_validated_before_reading() {
        ReaderSpy reader = new ReaderSpy(Optional.empty());
        assertThrows(NullPointerException.class,
                () -> new DefaultNodeDetailsUseCase(null, new ProjectNodeLookup(), new NodeDetailsMapper()));
        assertThrows(NullPointerException.class,
                () -> new DefaultNodeDetailsUseCase(reader, null, new NodeDetailsMapper()));
        assertThrows(NullPointerException.class,
                () -> new DefaultNodeDetailsUseCase(reader, new ProjectNodeLookup(), null));
        var useCase = useCase(reader);
        assertThrows(NullPointerException.class, () -> useCase.execute(null, "N"));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(" ", "N"));
        assertThrows(NullPointerException.class, () -> useCase.execute("P", null));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute("P", "\t"));
        assertEquals(0, reader.calls.get());
    }

    @Test
    void reader_and_downstream_runtime_failures_propagate_without_typed_conversion() {
        ProjectPersistenceException persistence = new ProjectPersistenceException("offline");
        ProjectReader failed = id -> { throw persistence; };
        assertSame(persistence, assertThrows(ProjectPersistenceException.class,
                () -> useCase(failed).execute("P", "N")));

        IllegalStateException defect = new IllegalStateException("reader defect");
        ProjectReader defective = id -> { throw defect; };
        assertSame(defect, assertThrows(IllegalStateException.class,
                () -> useCase(defective).execute("P", "N")));
        assertThrows(NullPointerException.class, () -> useCase(id -> null).execute("P", "N"));

        Node invalidLookupNode = node(null, "CHECK", "name");
        assertThrows(NullPointerException.class, () -> useCase(
                new ReaderSpy(Optional.of(project("P", List.of(invalidLookupNode))))).execute("P", "N"));

        Node invalidMapperNode = node("N", "CHECK", null);
        assertThrows(NullPointerException.class, () -> useCase(
                new ReaderSpy(Optional.of(project("P", List.of(invalidMapperNode))))).execute("P", "N"));
    }

    private static DefaultNodeDetailsUseCase useCase(ProjectReader reader) {
        return new DefaultNodeDetailsUseCase(reader, new ProjectNodeLookup(), new NodeDetailsMapper());
    }

    static Node node(String id, String type, String name) {
        return new Node(id, type, name, "description", "status", List.of(), List.of(), Map.of(), Map.of());
    }

    static Project project(String id, List<Node> nodes) {
        return new Project("contract", "schema", new Metadata(id, "project", null, null, Map.of()), List.of(),
                new Subject("local"), nodes, List.of(), new EvidenceManifest("evidence", "source", Map.of(),
                "normalization", "canonicalization", "fingerprint", List.of(), List.of(), List.of()),
                List.of(), Map.of());
    }

    private static final class ReaderSpy implements ProjectReader {
        final Optional<Project> result;
        final AtomicInteger calls = new AtomicInteger();
        final AtomicReference<String> id = new AtomicReference<>();
        ReaderSpy(Optional<Project> result) { this.result = result; }
        public Optional<Project> findById(String projectId) {
            calls.incrementAndGet();
            id.set(projectId);
            return result;
        }
    }
}
