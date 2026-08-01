package ru.kuznetsov.qaip.cli;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class QaipCliRuntimeInitializationTest {
    @Test
    void malformed_grammar_returns_usage_without_initializing_runtime() {
        AtomicInteger calls = new AtomicInteger();
        QaipCliApplicationTest.Streams streams = new QaipCliApplicationTest.Streams();

        int exitCode = QaipCliApplication.runWithRuntime(
                new String[]{"trace", "P"}, streams.out, streams.err, () -> {
            calls.incrementAndGet();
            throw new AssertionError("runtime must not be initialized");
        });

        assertEquals(CliExitCode.INVALID_USAGE.value(), exitCode);
        assertEquals(0, calls.get());
        assertFalse(streams.stderr().isBlank());
    }

    @Test
    void every_valid_command_attempts_runtime_composition_once() {
        for (String[] args : new String[][]{
                {"summary", "P"},
                {"show", "node", "P", "N"},
                {"show", "relationships", "P", "N"},
                {"trace", "P", "N"},
                {"validate", "project", "P"}}) {
            AtomicInteger calls = new AtomicInteger();
            QaipCliApplicationTest.Streams streams = new QaipCliApplicationTest.Streams();

            int exitCode = QaipCliApplication.runWithRuntime(args, streams.out, streams.err, () -> {
                calls.incrementAndGet();
                throw new IllegalStateException("secret-password");
            });

            assertEquals(CliExitCode.APPLICATION_FAILURE.value(), exitCode);
            assertEquals(1, calls.get());
            assertEquals("Database configuration is missing or invalid." + System.lineSeparator(),
                    streams.stderr());
            assertFalse(streams.stderr().contains("secret-password"));
            assertFalse(streams.stderr().contains("IllegalStateException"));
        }
    }
}
