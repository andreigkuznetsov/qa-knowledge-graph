package ru.kuznetsov.qaip.cli;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryNotFound;
import ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryUseCase;

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
                    "  qaip show node <project-id> <node-id>") + System.lineSeparator(), streams.stderr());
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
