package ru.kuznetsov.qaip.cli;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.node.ProjectNodeLookup;
import ru.kuznetsov.qaip.core.application.query.nodedetails.*;
import ru.kuznetsov.qaip.core.application.query.projectsummary.*;
import ru.kuznetsov.qaip.core.application.query.relationship.*;
import ru.kuznetsov.qaip.core.application.query.trace.*;
import ru.kuznetsov.qaip.core.application.query.validation.*;
import ru.kuznetsov.qaip.core.application.validation.ValidationEngine;
import ru.kuznetsov.qaip.core.application.validation.rule.*;
import ru.kuznetsov.qaip.core.domain.*;
import ru.kuznetsov.qaip.core.persistence.memory.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ValidationDispatchIntegrationTest {
    @Test
    void central_dispatch_preserves_id_and_isolates_all_five_paths() {
        AtomicInteger calls = new AtomicInteger();
        ProjectSummaryUseCase summary = id -> { calls.addAndGet(10000); return new ProjectSummaryNotFound(id); };
        NodeDetailsUseCase node = (p, n) -> { calls.addAndGet(1000); return new NodeDetailsNodeNotFound(p, n); };
        RelationshipsUseCase rel = (p, n) -> { calls.addAndGet(100); return new RelationshipsNodeNotFound(p, n); };
        TraceUseCase trace = (p, n) -> { calls.addAndGet(10); return new TraceNodeNotFound(p, n); };
        ValidationUseCase validation = id -> {
            calls.incrementAndGet();
            assertEquals(" P ", id);
            return new ValidationProjectNotFound(id);
        };
        QaipCliApplicationTest.Streams streams = new QaipCliApplicationTest.Streams();
        assertEquals(3, QaipCliApplication.run(new String[]{"validate", "project", " P "}, streams.out,
                streams.err, summary, node, rel, trace, validation));
        assertEquals(1, calls.get());
    }

    @Test
    void malformed_and_unknown_forms_invoke_no_use_case() {
        for (String[] args : new String[][]{{"validate"}, {"validate", "project"}, {"validate", "P"},
                {"validate", "project", "P", "extra"}, {"validate", "node", "P"},
                {"VALIDATE", "PROJECT", "P"}, {"unknown"}}) {
            AtomicInteger calls = new AtomicInteger();
            ProjectSummaryUseCase s = id -> { calls.incrementAndGet(); return new ProjectSummaryNotFound(id); };
            NodeDetailsUseCase n = (p, id) -> { calls.incrementAndGet(); return new NodeDetailsNodeNotFound(p, id); };
            RelationshipsUseCase r = (p, id) -> { calls.incrementAndGet(); return new RelationshipsNodeNotFound(p, id); };
            TraceUseCase t = (p, id) -> { calls.incrementAndGet(); return new TraceNodeNotFound(p, id); };
            ValidationUseCase v = id -> { calls.incrementAndGet(); return new ValidationProjectNotFound(id); };
            QaipCliApplicationTest.Streams streams = new QaipCliApplicationTest.Streams();
            assertEquals(2, QaipCliApplication.run(args, streams.out, streams.err, s, n, r, t, v));
            assertEquals(0, calls.get());
        }
    }

    @Test
    void real_same_process_validation_covers_clean_warning_error_mixed_missing_and_repeat() {
        InMemoryProjectRepository repository = new InMemoryProjectRepository();
        Project clean = project("CLEAN", List.of(node("S", "SCENARIO"), node("T", "TEST_IMPLEMENTATION")),
                List.of(relationship("R", "T", "VALIDATES", "S")));
        Project warning = project("WARNING", List.of(node("I", "BUSINESS_RULE")), List.of());
        Project error = project("ERROR", List.of(node("S", "SCENARIO"), node("N", "BUSINESS_RULE")),
                List.of(relationship("R", "N", "RELATED_TO", "S")));
        Project mixed = project("MIXED", List.of(node("S", "SCENARIO"), node("I", "BUSINESS_RULE")), List.of());
        Project before = mixed;
        for (Project project : List.of(clean, warning, error, mixed)) repository.insertIfAbsent(project);
        InMemoryProjectReader reader = new InMemoryProjectReader(repository);
        ValidationUseCase validation = new DefaultValidationUseCase(reader, new ValidationEngine(List.of(
                new IsolatedNodeValidationRule(), new ScenarioWithoutTestValidationRule())),
                new ValidationReportMapper());
        ProjectSummaryUseCase summary = new DefaultProjectSummaryUseCase(reader, new ProjectSummaryMapper());
        NodeDetailsUseCase node = new DefaultNodeDetailsUseCase(reader, new ProjectNodeLookup(), new NodeDetailsMapper());
        RelationshipsUseCase rel = new DefaultRelationshipsUseCase(reader, new ProjectNodeLookup(),
                new ProjectRelationshipLookup(), new RelationshipDetailsMapper());
        TraceUseCase trace = new DefaultTraceUseCase(reader, new ProjectNodeLookup(), new TraceGraphBuilder(), new TraceMapper());

        assertRun(0, "CLEAN", "Status: VALID", summary, node, rel, trace, validation);
        assertRun(0, "WARNING", "Warnings: 1", summary, node, rel, trace, validation);
        assertRun(0, "ERROR", "Status: INVALID", summary, node, rel, trace, validation);
        String first = assertRun(0, "MIXED", "Warnings: 2", summary, node, rel, trace, validation);
        assertEquals(first, assertRun(0, "MIXED", "Warnings: 2", summary, node, rel, trace, validation));
        assertRun(3, "MISSING", "Project not found", summary, node, rel, trace, validation);
        assertEquals(before, mixed);
    }

    private static String assertRun(int code, String id, String expected, ProjectSummaryUseCase s,
                                    NodeDetailsUseCase n, RelationshipsUseCase r, TraceUseCase t,
                                    ValidationUseCase v) {
        QaipCliApplicationTest.Streams streams = new QaipCliApplicationTest.Streams();
        assertEquals(code, QaipCliApplication.run(new String[]{"validate", "project", id}, streams.out,
                streams.err, s, n, r, t, v));
        assertTrue(streams.stdout().contains(expected));
        return streams.stdout();
    }

    private static Node node(String id, String type) {
        return new Node(id, type, id, null, null, List.of(), List.of(), Map.of(), Map.of());
    }
    private static Relationship relationship(String id, String from, String type, String to) {
        return new Relationship(id, from, type, to, Map.of(), List.of());
    }
    private static Project project(String id, List<Node> nodes, List<Relationship> relationships) {
        return new Project("contract", "schema", new Metadata(id, id, null, null, Map.of()), List.of(),
                new Subject("local"), nodes, relationships, new EvidenceManifest("e", "s", Map.of(),
                "n", "c", "f", List.of(), List.of(), List.of()), List.of(), Map.of());
    }
}
