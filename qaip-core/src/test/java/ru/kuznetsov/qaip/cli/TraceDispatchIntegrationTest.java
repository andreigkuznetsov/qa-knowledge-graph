package ru.kuznetsov.qaip.cli;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.node.ProjectNodeLookup;
import ru.kuznetsov.qaip.core.application.query.nodedetails.*;
import ru.kuznetsov.qaip.core.application.query.projectsummary.*;
import ru.kuznetsov.qaip.core.application.query.relationship.*;
import ru.kuznetsov.qaip.core.application.query.trace.*;
import ru.kuznetsov.qaip.core.domain.*;
import ru.kuznetsov.qaip.core.persistence.memory.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TraceDispatchIntegrationTest {
    @Test
    void central_dispatch_preserves_trace_ids_and_isolates_all_four_paths() {
        AtomicInteger summary = new AtomicInteger();
        AtomicInteger node = new AtomicInteger();
        AtomicInteger relationships = new AtomicInteger();
        AtomicInteger trace = new AtomicInteger();
        ProjectSummaryUseCase summaryUseCase = id -> { summary.incrementAndGet(); return new ProjectSummaryNotFound(id); };
        NodeDetailsUseCase nodeUseCase = (p, n) -> { node.incrementAndGet(); return new NodeDetailsNodeNotFound(p, n); };
        RelationshipsUseCase relationshipsUseCase = (p, n) -> { relationships.incrementAndGet(); return new RelationshipsNodeNotFound(p, n); };
        TraceUseCase traceUseCase = (p, n) -> {
            trace.incrementAndGet();
            assertEquals(" P ", p);
            assertEquals(" N ", n);
            return new TraceNodeNotFound(p, n);
        };
        QaipCliApplicationTest.Streams streams = new QaipCliApplicationTest.Streams();
        assertEquals(5, QaipCliApplication.run(new String[]{"trace", " P ", " N "}, streams.out, streams.err,
                summaryUseCase, nodeUseCase, relationshipsUseCase, traceUseCase));
        assertEquals(List.of(0, 0, 0, 1), List.of(summary.get(), node.get(), relationships.get(), trace.get()));
    }

    @Test
    void malformed_and_unknown_forms_invoke_no_use_case() {
        for (String[] args : new String[][]{{"trace"}, {"trace", "P"}, {"trace", "P", "N", "extra"},
                {"show", "trace", "P", "N"}, {"TRACE", "P", "N"}, {"unknown"}}) {
            AtomicInteger calls = new AtomicInteger();
            ProjectSummaryUseCase s = id -> { calls.incrementAndGet(); return new ProjectSummaryNotFound(id); };
            NodeDetailsUseCase n = (p, id) -> { calls.incrementAndGet(); return new NodeDetailsNodeNotFound(p, id); };
            RelationshipsUseCase r = (p, id) -> { calls.incrementAndGet(); return new RelationshipsNodeNotFound(p, id); };
            TraceUseCase t = (p, id) -> { calls.incrementAndGet(); return new TraceNodeNotFound(p, id); };
            QaipCliApplicationTest.Streams streams = new QaipCliApplicationTest.Streams();
            assertEquals(2, QaipCliApplication.run(args, streams.out, streams.err, s, n, r, t));
            assertEquals(0, calls.get());
        }
    }

    @Test
    void real_same_process_trace_handles_cycle_isolated_and_absence() {
        InMemoryProjectRepository repository = new InMemoryProjectRepository();
        Project project = project();
        repository.insertIfAbsent(project);
        InMemoryProjectReader reader = new InMemoryProjectReader(repository);
        TraceUseCase trace = new DefaultTraceUseCase(reader, new ProjectNodeLookup(),
                new TraceGraphBuilder(), new TraceMapper());
        ProjectSummaryUseCase summary = new DefaultProjectSummaryUseCase(reader, new ProjectSummaryMapper());
        NodeDetailsUseCase node = new DefaultNodeDetailsUseCase(reader, new ProjectNodeLookup(), new NodeDetailsMapper());
        RelationshipsUseCase rel = new DefaultRelationshipsUseCase(reader, new ProjectNodeLookup(),
                new ProjectRelationshipLookup(), new RelationshipDetailsMapper());

        assertRun(0, new String[]{"trace", "P", "A"}, summary, node, rel, trace, "AB | LINK | A -> B");
        assertRun(0, new String[]{"trace", "P", "ISO"}, summary, node, rel, trace, "(none)");
        assertRun(3, new String[]{"trace", "missing", "A"}, summary, node, rel, trace, "Project not found");
        assertRun(5, new String[]{"trace", "P", "missing"}, summary, node, rel, trace, "Node not found");
    }

    private static void assertRun(int code, String[] args, ProjectSummaryUseCase s, NodeDetailsUseCase n,
                                  RelationshipsUseCase r, TraceUseCase t, String output) {
        QaipCliApplicationTest.Streams streams = new QaipCliApplicationTest.Streams();
        assertEquals(code, QaipCliApplication.run(args, streams.out, streams.err, s, n, r, t));
        assertTrue(streams.stdout().contains(output));
    }

    private static Project project() {
        Node a = new Node("A", "STORY", "A", null, null, List.of(), List.of(), Map.of(), Map.of());
        Node b = new Node("B", "TEST", "B", null, null, List.of(), List.of(), Map.of(), Map.of());
        Node isolated = new Node("ISO", "RULE", "ISO", null, null, List.of(), List.of(), Map.of(), Map.of());
        Relationship ab = new Relationship("AB", "A", "LINK", "B", Map.of(), List.of());
        Relationship ba = new Relationship("BA", "B", "BACK", "A", Map.of(), List.of());
        return new Project("contract", "schema", new Metadata("P", "P", null, null, Map.of()), List.of(),
                new Subject("local"), List.of(a, b, isolated), List.of(ab, ba),
                new EvidenceManifest("e", "s", Map.of(), "n", "c", "f", List.of(), List.of(), List.of()),
                List.of(), Map.of());
    }
}
