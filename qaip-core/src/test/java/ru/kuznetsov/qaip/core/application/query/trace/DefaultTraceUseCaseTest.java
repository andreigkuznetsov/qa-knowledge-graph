package ru.kuznetsov.qaip.core.application.query.trace;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.node.ProjectNodeLookup;
import ru.kuznetsov.qaip.core.domain.*;
import ru.kuznetsov.qaip.core.persistence.ProjectPersistenceException;
import ru.kuznetsov.qaip.core.persistence.read.ProjectReader;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DefaultTraceUseCaseTest {
    @Test
    void success_preserves_exact_ids_and_mapped_result() {
        Project project = project(" P ", List.of(node(" N ", "story")), List.of());
        CountingReader reader = new CountingReader(Optional.of(project));
        TraceQueryResult result = useCase(reader).execute(" P ", " N ");
        TraceFound found = assertInstanceOf(TraceFound.class, result);
        assertEquals(" P ", found.projectId());
        assertEquals(" N ", found.startNodeId());
        assertEquals(new TraceResult(" N ", List.of(new TraceNode(" N ", "story")), List.of()), found.trace());
        assertEquals(1, reader.calls);
        assertEquals(" P ", reader.lastProjectId);
    }

    @Test
    void project_absence_and_node_absence_return_exact_typed_results() {
        CountingReader absentReader = new CountingReader(Optional.empty());
        assertEquals(new TraceProjectNotFound("missing"), useCase(absentReader).execute("missing", "N"));
        assertEquals(1, absentReader.calls);

        CountingReader presentReader = new CountingReader(Optional.of(project("P", List.of(node("A", "type")), List.of())));
        assertEquals(new TraceNodeNotFound("P", "missing"), useCase(presentReader).execute("P", "missing"));
        assertEquals(1, presentReader.calls);
    }

    @Test
    void constructor_and_inputs_enforce_contract_before_reader_invocation() {
        ProjectNodeLookup lookup = new ProjectNodeLookup();
        TraceGraphBuilder builder = new TraceGraphBuilder();
        TraceMapper mapper = new TraceMapper();
        ProjectReader reader = id -> Optional.empty();
        assertThrows(NullPointerException.class, () -> new DefaultTraceUseCase(null, lookup, builder, mapper));
        assertThrows(NullPointerException.class, () -> new DefaultTraceUseCase(reader, null, builder, mapper));
        assertThrows(NullPointerException.class, () -> new DefaultTraceUseCase(reader, lookup, null, mapper));
        assertThrows(NullPointerException.class, () -> new DefaultTraceUseCase(reader, lookup, builder, null));

        CountingReader counting = new CountingReader(Optional.empty());
        TraceUseCase useCase = useCase(counting);
        assertThrows(NullPointerException.class, () -> useCase.execute(null, "N"));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(" ", "N"));
        assertThrows(NullPointerException.class, () -> useCase.execute("P", null));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute("P", "\t"));
        assertEquals(0, counting.calls);
    }

    @Test
    void reader_null_and_persistence_failure_propagate_without_typed_conversion() {
        ProjectReader nullReader = id -> null;
        assertThrows(NullPointerException.class, () -> useCase(nullReader).execute("P", "N"));
        ProjectPersistenceException failure = new ProjectPersistenceException("read failed");
        ProjectReader failingReader = id -> { throw failure; };
        assertSame(failure, assertThrows(ProjectPersistenceException.class,
                () -> useCase(failingReader).execute("P", "N")));
    }

    @Test
    void builder_and_mapper_failures_propagate_without_partial_results() {
        Project dangling = project("P", List.of(node("A", "type")),
                List.of(relationship("R", "A", "MISSING", "link")));
        assertThrows(IllegalStateException.class,
                () -> useCase(id -> Optional.of(dangling)).execute("P", "A"));

        Project invalidMapping = project("P", List.of(node("A", null)), List.of());
        assertThrows(NullPointerException.class,
                () -> useCase(id -> Optional.of(invalidMapping)).execute("P", "A"));
    }

    @Test
    void result_records_validate_values_and_have_value_semantics() {
        TraceResult trace = new TraceResult("N", List.of(new TraceNode("N", "type")), List.of());
        assertEquals(new TraceFound("P", "N", trace), new TraceFound("P", "N", trace));
        assertEquals(new TraceFound("P", "N", trace).hashCode(), new TraceFound("P", "N", trace).hashCode());
        assertThrows(NullPointerException.class, () -> new TraceFound(null, "N", trace));
        assertThrows(IllegalArgumentException.class, () -> new TraceFound("P", " ", trace));
        assertThrows(NullPointerException.class, () -> new TraceFound("P", "N", null));
        assertThrows(IllegalArgumentException.class, () -> new TraceProjectNotFound(""));
        assertThrows(NullPointerException.class, () -> new TraceNodeNotFound("P", null));
    }

    private static DefaultTraceUseCase useCase(ProjectReader reader) {
        return new DefaultTraceUseCase(reader, new ProjectNodeLookup(), new TraceGraphBuilder(), new TraceMapper());
    }

    private static final class CountingReader implements ProjectReader {
        private final Optional<Project> result;
        private int calls;
        private String lastProjectId;
        private CountingReader(Optional<Project> result) { this.result = result; }
        @Override public Optional<Project> findById(String projectId) {
            calls++;
            lastProjectId = projectId;
            return result;
        }
    }

    static Node node(String id, String type) {
        return new Node(id, type, id, null, null, List.of(), List.of(), Map.of(), Map.of());
    }

    static Relationship relationship(String id, String from, String to, String type) {
        return new Relationship(id, from, type, to, Map.of(), List.of());
    }

    static Project project(String id, List<Node> nodes, List<Relationship> relationships) {
        return new Project("contract", "schema", new Metadata(id, id, null, null, Map.of()), List.of(),
                new Subject("local"), nodes, relationships, new EvidenceManifest("evidence", "source", Map.of(),
                "normalization", "canonicalization", "fingerprint", List.of(), List.of(), List.of()),
                List.of(), Map.of());
    }
}
