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
                {"validate", "project", "P"},
                {"import", "project.json"}}) {
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

    @Test
    void bootstrap_failure_prevents_command_execution_and_is_reported_safely() {
        AtomicInteger dataSourceCalls = new AtomicInteger();
        AtomicInteger bootstrapCalls = new AtomicInteger();
        QaipCliApplicationTest.Streams streams = new QaipCliApplicationTest.Streams();

        int exitCode = QaipCliApplication.runWithRuntime(
                new String[]{"summary", "P"}, streams.out, streams.err,
                () -> RuntimeComposition.create(() -> {
                    dataSourceCalls.incrementAndGet();
                    return null;
                }, dataSource -> {
                    bootstrapCalls.incrementAndGet();
                    throw new IllegalStateException("credential-value");
                }));

        assertEquals(CliExitCode.APPLICATION_FAILURE.value(), exitCode);
        assertEquals(1, dataSourceCalls.get());
        assertEquals(1, bootstrapCalls.get());
        assertEquals("", streams.stdout());
        assertEquals("Database configuration is missing or invalid." + System.lineSeparator(), streams.stderr());
        assertFalse(streams.stderr().contains("credential-value"));
    }
}
