package ru.kuznetsov.qaip.cli;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryNotFound;
import ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryUseCase;
import ru.kuznetsov.qaip.core.application.query.nodedetails.NodeDetailsNodeNotFound;
import ru.kuznetsov.qaip.core.application.query.nodedetails.NodeDetailsUseCase;
import ru.kuznetsov.qaip.core.application.query.relationship.RelationshipsNodeNotFound;
import ru.kuznetsov.qaip.core.application.query.relationship.RelationshipsUseCase;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class QaipCliApplicationTest {
    @Test
    void accepts_exact_summary_command_and_preserves_id() {
        SpyUseCase useCase = new SpyUseCase();
        Streams streams = new Streams();
        int code = QaipCliApplication.run(new String[]{"summary", " P-Id "}, streams.out, streams.err, useCase);
        assertEquals(3, code);
        assertEquals(1, useCase.calls.get());
        assertEquals(" P-Id ", useCase.id.get());
        assertEquals("Project not found:  P-Id " + System.lineSeparator(), streams.stdout());
        assertEquals("", streams.stderr());
    }

    @Test
    void rejects_every_invalid_shape_before_use_case_invocation() {
        for (String[] args : new String[][]{{}, {"summary"}, {"summary", "P", "extra"},
                {"unknown", "P"}, {"Summary", "P"}}) {
            SpyUseCase useCase = new SpyUseCase();
            Streams streams = new Streams();
            assertEquals(2, QaipCliApplication.run(args, streams.out, streams.err, useCase));
            assertEquals(0, useCase.calls.get());
            assertEquals("", streams.stdout());
            assertEquals(String.join(System.lineSeparator(), "Usage:", "  qaip summary <project-id>",
                    "  qaip show node <project-id> <node-id>",
                    "  qaip show relationships <project-id> <node-id>",
                    "  qaip trace <project-id> <start-node-id>",
                    "  qaip validate project <project-id>",
                    "  qaip import <file>") + System.lineSeparator(), streams.stderr());
        }
    }

    @Test
    void centralized_dispatch_isolates_three_exact_command_paths_and_rejects_malformed_relationships() {
        for (String[] invalid : new String[][]{{"show", "relationships"}, {"show", "relationships", "P"},
                {"show", "relationships", "P", "N", "extra"}, {"relationships", "P", "N"},
                {"show", "relationship", "P", "N"}, {"SHOW", "RELATIONSHIPS", "P", "N"},
                {"unknown"}}) {
            DispatchSpies spies = new DispatchSpies();
            Streams streams = new Streams();
            assertEquals(2, run(invalid, streams, spies));
            assertEquals(0, spies.summary.calls.get());
            assertEquals(0, spies.node.calls.get());
            assertEquals(0, spies.relationships.calls.get());
        }

        DispatchSpies summary = new DispatchSpies();
        assertEquals(3, run(new String[]{"summary", " P "}, new Streams(), summary));
        assertEquals(1, summary.summary.calls.get());
        assertEquals(0, summary.node.calls.get());
        assertEquals(0, summary.relationships.calls.get());

        DispatchSpies node = new DispatchSpies();
        assertEquals(5, run(new String[]{"show", "node", " P ", " N "}, new Streams(), node));
        assertEquals(0, node.summary.calls.get());
        assertEquals(1, node.node.calls.get());
        assertEquals(0, node.relationships.calls.get());

        DispatchSpies relationships = new DispatchSpies();
        assertEquals(5, run(new String[]{"show", "relationships", " P ", " N "},
                new Streams(), relationships));
        assertEquals(0, relationships.summary.calls.get());
        assertEquals(0, relationships.node.calls.get());
        assertEquals(1, relationships.relationships.calls.get());
        assertEquals(" P ", relationships.relationships.projectId.get());
        assertEquals(" N ", relationships.relationships.nodeId.get());
    }

    private static int run(String[] args, Streams streams, DispatchSpies spies) {
        return QaipCliApplication.run(args, streams.out, streams.err,
                spies.summary, spies.node, spies.relationships);
    }

    private static final class DispatchSpies {
        final SpyUseCase summary = new SpyUseCase();
        final SpyNodeUseCase node = new SpyNodeUseCase();
        final RelationshipsCliCommandTest.StubUseCase relationships =
                new RelationshipsCliCommandTest.StubUseCase(new RelationshipsNodeNotFound(" P ", " N "), null);
    }

    private static final class SpyNodeUseCase implements NodeDetailsUseCase {
        final AtomicInteger calls = new AtomicInteger();
        public ru.kuznetsov.qaip.core.application.query.nodedetails.NodeDetailsQueryResult execute(
                String projectId, String nodeId) {
            calls.incrementAndGet();
            return new NodeDetailsNodeNotFound(projectId, nodeId);
        }
    }

    private static final class SpyUseCase implements ProjectSummaryUseCase {
        final AtomicInteger calls = new AtomicInteger();
        final AtomicReference<String> id = new AtomicReference<>();
        public ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryQueryResult execute(String value) {
            calls.incrementAndGet();
            id.set(value);
            return new ProjectSummaryNotFound(value);
        }
    }

    static final class Streams {
        private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        private final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        final PrintStream out = new PrintStream(stdout, true, StandardCharsets.UTF_8);
        final PrintStream err = new PrintStream(stderr, true, StandardCharsets.UTF_8);
        String stdout() { return stdout.toString(StandardCharsets.UTF_8); }
        String stderr() { return stderr.toString(StandardCharsets.UTF_8); }
    }
}
