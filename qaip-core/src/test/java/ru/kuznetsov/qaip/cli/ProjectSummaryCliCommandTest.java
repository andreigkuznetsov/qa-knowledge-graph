package ru.kuznetsov.qaip.cli;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.projectsummary.*;
import ru.kuznetsov.qaip.core.persistence.ProjectPersistenceException;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ProjectSummaryCliCommandTest {
    @Test
    void found_invokes_once_renders_to_out_and_returns_success() {
        ProjectSummaryResult summary = new ProjectSummaryResult("P", "C", "S", 1, 2, 3, 4, 5);
        StubUseCase useCase = new StubUseCase(new ProjectSummaryFound(summary), null);
        QaipCliApplicationTest.Streams streams = new QaipCliApplicationTest.Streams();
        int code = command(useCase).execute("P", streams.out, streams.err);
        assertEquals(0, code);
        assertEquals(1, useCase.calls.get());
        assertEquals("P", useCase.id.get());
        assertEquals(new ProjectSummaryTextRenderer().renderFound(summary) + System.lineSeparator(), streams.stdout());
        assertEquals("", streams.stderr());
    }

    @Test
    void not_found_invokes_once_renders_only_absence_and_returns_three() {
        StubUseCase useCase = new StubUseCase(new ProjectSummaryNotFound("P"), null);
        QaipCliApplicationTest.Streams streams = new QaipCliApplicationTest.Streams();
        assertEquals(3, command(useCase).execute("P", streams.out, streams.err));
        assertEquals(1, useCase.calls.get());
        assertEquals("Project not found: P" + System.lineSeparator(), streams.stdout());
        assertEquals("", streams.stderr());
        assertFalse(streams.stdout().contains("Sources:"));
    }

    @Test
    void maps_validation_persistence_and_unexpected_failures_without_stack_traces() {
        assertFailure(new IllegalArgumentException("projectId must not be blank"), 2,
                "Invalid project ID: projectId must not be blank");
        assertFailure(new ProjectPersistenceException("database unavailable"), 4,
                "Project summary failed: database unavailable");
        assertFailure(new IllegalStateException("internal detail"), 4, "Project summary failed.");
    }

    private static void assertFailure(RuntimeException failure, int expectedCode, String expectedError) {
        StubUseCase useCase = new StubUseCase(null, failure);
        QaipCliApplicationTest.Streams streams = new QaipCliApplicationTest.Streams();
        assertEquals(expectedCode, command(useCase).execute("P", streams.out, streams.err));
        assertEquals(1, useCase.calls.get());
        assertEquals("", streams.stdout());
        assertEquals(expectedError + System.lineSeparator(), streams.stderr());
        assertFalse(streams.stderr().contains("\tat "));
    }

    private static ProjectSummaryCliCommand command(ProjectSummaryUseCase useCase) {
        return new ProjectSummaryCliCommand(useCase, new ProjectSummaryTextRenderer());
    }

    private static final class StubUseCase implements ProjectSummaryUseCase {
        final ProjectSummaryQueryResult result;
        final RuntimeException failure;
        final AtomicInteger calls = new AtomicInteger();
        final AtomicReference<String> id = new AtomicReference<>();
        StubUseCase(ProjectSummaryQueryResult result, RuntimeException failure) {
            this.result = result;
            this.failure = failure;
        }
        public ProjectSummaryQueryResult execute(String projectId) {
            calls.incrementAndGet();
            id.set(projectId);
            if (failure != null) throw failure;
            return result;
        }
    }
}
