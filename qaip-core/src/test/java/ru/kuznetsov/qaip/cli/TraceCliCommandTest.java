package ru.kuznetsov.qaip.cli;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.trace.*;
import ru.kuznetsov.qaip.core.persistence.ProjectPersistenceException;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TraceCliCommandTest {
    @Test
    void invokes_once_and_renders_found_including_isolated_trace() {
        AtomicInteger calls = new AtomicInteger();
        TraceResult trace = new TraceResult(" N ", List.of(new TraceNode(" N ", " TYPE ")), List.of());
        TraceUseCase useCase = (projectId, nodeId) -> {
            calls.incrementAndGet();
            assertEquals(" P ", projectId);
            assertEquals(" N ", nodeId);
            return new TraceFound(projectId, nodeId, trace);
        };
        QaipCliApplicationTest.Streams streams = new QaipCliApplicationTest.Streams();
        assertEquals(0, new TraceCliCommand(useCase, new TraceTextRenderer())
                .execute(" P ", " N ", streams.out, streams.err));
        assertEquals(1, calls.get());
        assertTrue(streams.stdout().contains(" N  |  TYPE "));
        assertTrue(streams.stdout().contains("Relationships:" + System.lineSeparator() + "(none)"));
        assertEquals("", streams.stderr());
    }

    @Test
    void maps_typed_absence_to_existing_outputs_and_codes() {
        QaipCliApplicationTest.Streams projectStreams = new QaipCliApplicationTest.Streams();
        assertEquals(3, command((p, n) -> new TraceProjectNotFound(p))
                .execute("P", "N", projectStreams.out, projectStreams.err));
        assertEquals("Project not found: P" + System.lineSeparator(), projectStreams.stdout());

        QaipCliApplicationTest.Streams nodeStreams = new QaipCliApplicationTest.Streams();
        assertEquals(5, command((p, n) -> new TraceNodeNotFound(p, n))
                .execute("P", "N", nodeStreams.out, nodeStreams.err));
        assertEquals("Node not found: N in project P" + System.lineSeparator(), nodeStreams.stdout());
    }

    @Test
    void failures_are_safe_and_never_print_stack_traces() {
        QaipCliApplicationTest.Streams persistence = new QaipCliApplicationTest.Streams();
        assertEquals(4, command((p, n) -> { throw new ProjectPersistenceException("database unavailable"); })
                .execute("P", "N", persistence.out, persistence.err));
        assertEquals("Trace failed: database unavailable" + System.lineSeparator(), persistence.stderr());

        QaipCliApplicationTest.Streams unexpected = new QaipCliApplicationTest.Streams();
        assertEquals(4, command((p, n) -> { throw new IllegalStateException("secret"); })
                .execute("P", "N", unexpected.out, unexpected.err));
        assertEquals("Trace failed." + System.lineSeparator(), unexpected.stderr());
        assertFalse(unexpected.stderr().contains("Exception"));
        assertFalse(unexpected.stderr().contains("secret"));
    }

    private static TraceCliCommand command(TraceUseCase useCase) {
        return new TraceCliCommand(useCase, new TraceTextRenderer());
    }
}
