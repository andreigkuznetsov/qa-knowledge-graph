package ru.kuznetsov.qaip.cli;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.nodedetails.*;
import ru.kuznetsov.qaip.core.persistence.ProjectPersistenceException;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class NodeDetailsCliCommandTest {
    @Test
    void found_invokes_once_renders_all_fields_and_returns_success() {
        NodeDetailsResult details = new NodeDetailsResult("N", "CHECK", "name", null, null);
        StubUseCase useCase = new StubUseCase(new NodeDetailsFound(details), null);
        QaipCliApplicationTest.Streams streams = new QaipCliApplicationTest.Streams();
        assertEquals(0, command(useCase).execute("P", "N", streams.out, streams.err));
        assertEquals(1, useCase.calls.get());
        assertEquals(new NodeDetailsTextRenderer().renderFound("P", details) + System.lineSeparator(),
                streams.stdout());
        assertEquals("", streams.stderr());
    }

    @Test
    void renders_project_and_node_absence_with_distinct_existing_codes() {
        QaipCliApplicationTest.Streams project = new QaipCliApplicationTest.Streams();
        assertEquals(3, command(new StubUseCase(new NodeDetailsProjectNotFound("P"), null))
                .execute("P", "N", project.out, project.err));
        assertEquals("Project not found: P" + System.lineSeparator(), project.stdout());

        QaipCliApplicationTest.Streams node = new QaipCliApplicationTest.Streams();
        assertEquals(5, command(new StubUseCase(new NodeDetailsNodeNotFound("P", "N"), null))
                .execute("P", "N", node.out, node.err));
        assertEquals("Node not found: N in project P" + System.lineSeparator(), node.stdout());
        assertEquals("", project.stderr());
        assertEquals("", node.stderr());
    }

    @Test
    void maps_validation_infrastructure_and_unexpected_failures_without_stack_traces() {
        assertFailure(new IllegalArgumentException("nodeId must not be blank"), 2,
                "Invalid node details request: nodeId must not be blank");
        assertFailure(new ProjectPersistenceException("database unavailable"), 4,
                "Node details failed: database unavailable");
        assertFailure(new IllegalStateException("internal detail"), 4, "Node details failed.");
    }

    private static void assertFailure(RuntimeException failure, int code, String error) {
        StubUseCase useCase = new StubUseCase(null, failure);
        QaipCliApplicationTest.Streams streams = new QaipCliApplicationTest.Streams();
        assertEquals(code, command(useCase).execute("P", "N", streams.out, streams.err));
        assertEquals(1, useCase.calls.get());
        assertEquals("", streams.stdout());
        assertEquals(error + System.lineSeparator(), streams.stderr());
        assertFalse(streams.stderr().contains("\tat "));
    }

    private static NodeDetailsCliCommand command(NodeDetailsUseCase useCase) {
        return new NodeDetailsCliCommand(useCase, new NodeDetailsTextRenderer());
    }

    private static final class StubUseCase implements NodeDetailsUseCase {
        final NodeDetailsQueryResult result;
        final RuntimeException failure;
        final AtomicInteger calls = new AtomicInteger();
        StubUseCase(NodeDetailsQueryResult result, RuntimeException failure) {
            this.result = result;
            this.failure = failure;
        }
        public NodeDetailsQueryResult execute(String projectId, String nodeId) {
            calls.incrementAndGet();
            if (failure != null) throw failure;
            return result;
        }
    }
}
