package ru.kuznetsov.qaip.cli;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.nodedetails.*;
import ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryNotFound;
import ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryQueryResult;
import ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryUseCase;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class NodeDetailsDispatchTest {
    @Test
    void central_dispatch_routes_both_commands_without_interference_and_preserves_ids() {
        SummarySpy summary = new SummarySpy();
        NodeSpy node = new NodeSpy();
        QaipCliApplicationTest.Streams summaryStreams = new QaipCliApplicationTest.Streams();
        assertEquals(3, QaipCliApplication.run(new String[]{"summary", " P "}, summaryStreams.out,
                summaryStreams.err, summary, node));
        assertEquals(1, summary.calls.get());
        assertEquals(0, node.calls.get());
        assertEquals(" P ", summary.projectId);

        QaipCliApplicationTest.Streams nodeStreams = new QaipCliApplicationTest.Streams();
        assertEquals(5, QaipCliApplication.run(new String[]{"show", "node", " P2 ", " N2 "}, nodeStreams.out,
                nodeStreams.err, summary, node));
        assertEquals(1, summary.calls.get());
        assertEquals(1, node.calls.get());
        assertEquals(" P2 ", node.projectId);
        assertEquals(" N2 ", node.nodeId);
    }

    @Test
    void malformed_show_and_unknown_commands_invoke_neither_use_case() {
        for (String[] args : new String[][]{{"show"}, {"show", "node"}, {"show", "node", "P"},
                {"show", "node", "P", "N", "extra"}, {"node", "P", "N"},
                {"show", "requirement", "P", "N"}, {"SHOW", "NODE", "P", "N"}}) {
            SummarySpy summary = new SummarySpy();
            NodeSpy node = new NodeSpy();
            QaipCliApplicationTest.Streams streams = new QaipCliApplicationTest.Streams();
            assertEquals(2, QaipCliApplication.run(args, streams.out, streams.err, summary, node));
            assertEquals(0, summary.calls.get());
            assertEquals(0, node.calls.get());
            assertTrue(streams.stderr().startsWith("Usage:"));
        }
    }

    private static final class SummarySpy implements ProjectSummaryUseCase {
        final AtomicInteger calls = new AtomicInteger();
        String projectId;
        public ProjectSummaryQueryResult execute(String value) {
            calls.incrementAndGet();
            projectId = value;
            return new ProjectSummaryNotFound(value);
        }
    }

    private static final class NodeSpy implements NodeDetailsUseCase {
        final AtomicInteger calls = new AtomicInteger();
        String projectId;
        String nodeId;
        public NodeDetailsQueryResult execute(String project, String node) {
            calls.incrementAndGet();
            projectId = project;
            nodeId = node;
            return new NodeDetailsNodeNotFound(project, node);
        }
    }
}
