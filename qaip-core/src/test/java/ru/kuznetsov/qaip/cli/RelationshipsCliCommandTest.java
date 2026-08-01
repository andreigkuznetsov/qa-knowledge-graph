package ru.kuznetsov.qaip.cli;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.relationship.*;
import ru.kuznetsov.qaip.core.persistence.ProjectPersistenceException;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RelationshipsCliCommandTest {
    @Test
    void found_invokes_once_preserves_ids_renders_and_returns_success() {
        var details = new RelationshipDetailsResult(List.of(), List.of());
        StubUseCase useCase = new StubUseCase(new RelationshipsFound(" P ", " N ", details), null);
        QaipCliApplicationTest.Streams streams = new QaipCliApplicationTest.Streams();
        assertEquals(0, command(useCase).execute(" P ", " N ", streams.out, streams.err));
        assertEquals(1, useCase.calls.get());
        assertEquals(" P ", useCase.projectId.get());
        assertEquals(" N ", useCase.nodeId.get());
        assertEquals(new RelationshipsTextRenderer().renderFound(" P ", " N ", details)
                + System.lineSeparator(), streams.stdout());
        assertEquals("", streams.stderr());
    }

    @Test
    void renders_both_absence_levels_with_existing_exit_codes() {
        QaipCliApplicationTest.Streams project = new QaipCliApplicationTest.Streams();
        assertEquals(3, command(new StubUseCase(new RelationshipsProjectNotFound("P"), null))
                .execute("P", "N", project.out, project.err));
        assertEquals("Project not found: P" + System.lineSeparator(), project.stdout());

        QaipCliApplicationTest.Streams node = new QaipCliApplicationTest.Streams();
        assertEquals(5, command(new StubUseCase(new RelationshipsNodeNotFound("P", "N"), null))
                .execute("P", "N", node.out, node.err));
        assertEquals("Node not found: N in project P" + System.lineSeparator(), node.stdout());
    }

    @Test
    void maps_validation_infrastructure_and_unexpected_failures_without_stack_traces() {
        assertFailure(new IllegalArgumentException("nodeId must not be blank"), 2,
                "Invalid relationships request: nodeId must not be blank");
        assertFailure(new ProjectPersistenceException("database unavailable"), 4,
                "Relationships failed: database unavailable");
        assertFailure(new IllegalStateException("internal detail"), 4, "Relationships failed.");
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

    private static RelationshipsCliCommand command(RelationshipsUseCase useCase) {
        return new RelationshipsCliCommand(useCase, new RelationshipsTextRenderer());
    }

    static final class StubUseCase implements RelationshipsUseCase {
        private final RelationshipsQueryResult result;
        private final RuntimeException failure;
        final AtomicInteger calls = new AtomicInteger();
        final AtomicReference<String> projectId = new AtomicReference<>();
        final AtomicReference<String> nodeId = new AtomicReference<>();

        StubUseCase(RelationshipsQueryResult result, RuntimeException failure) {
            this.result = result;
            this.failure = failure;
        }

        public RelationshipsQueryResult execute(String projectId, String nodeId) {
            calls.incrementAndGet();
            this.projectId.set(projectId);
            this.nodeId.set(nodeId);
            if (failure != null) throw failure;
            return result;
        }
    }
}
