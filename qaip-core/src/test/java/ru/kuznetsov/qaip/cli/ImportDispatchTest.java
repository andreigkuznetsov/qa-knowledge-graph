package ru.kuznetsov.qaip.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.kuznetsov.qaip.core.application.importproject.ImportProjectPersistenceFailed;
import ru.kuznetsov.qaip.core.application.importproject.ImportResultMapper;
import ru.kuznetsov.qaip.core.persistence.ProjectInserted;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImportDispatchTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void exact_import_grammar_dispatches_once_renders_once_and_writes_stdout() throws Exception {
        Path file = temporaryDirectory.resolve("project.json");
        Files.writeString(file, "{\"value\":\"Привет\"}", StandardCharsets.UTF_8);
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<String> received = new AtomicReference<>();
        RuntimeComposition composition = composition(new ImportCliCommand(source -> {
            calls.incrementAndGet();
            received.set(source.value());
            return new ImportProjectPersistenceFailed("safe message");
        }, new ImportResultMapper()));
        QaipCliApplicationTest.Streams streams = new QaipCliApplicationTest.Streams();

        int exitCode = QaipCliApplication.runWithRuntime(
                new String[]{"import", file.toString()}, streams.out, streams.err, () -> composition);

        assertEquals(CliExitCode.SUCCESS.value(), exitCode);
        assertEquals(1, calls.get());
        assertEquals("{\"value\":\"Привет\"}", received.get());
        assertEquals(String.join(System.lineSeparator(),
                "Import failed", "Message: safe message") + System.lineSeparator(), streams.stdout());
        assertEquals("", streams.stderr());
    }

    @Test
    void missing_and_extra_file_arguments_use_existing_usage_flow_without_runtime() {
        for (String[] args : new String[][]{{"import"}, {"import", "one.json", "two.json"}}) {
            AtomicInteger runtimeCalls = new AtomicInteger();
            QaipCliApplicationTest.Streams streams = new QaipCliApplicationTest.Streams();

            int exitCode = QaipCliApplication.runWithRuntime(args, streams.out, streams.err, () -> {
                runtimeCalls.incrementAndGet();
                throw new AssertionError("runtime must not be initialized");
            });

            assertEquals(CliExitCode.INVALID_USAGE.value(), exitCode);
            assertEquals(0, runtimeCalls.get());
            assertEquals("", streams.stdout());
            org.junit.jupiter.api.Assertions.assertTrue(streams.stderr().contains("  qaip import <file>"));
        }
    }

    @Test
    void file_failure_skips_application_and_renderer_output_and_uses_existing_failure_code() {
        AtomicInteger calls = new AtomicInteger();
        RuntimeComposition composition = composition(new ImportCliCommand(source -> {
            calls.incrementAndGet();
            return new ImportProjectPersistenceFailed("must not render");
        }, new ImportResultMapper()));
        QaipCliApplicationTest.Streams streams = new QaipCliApplicationTest.Streams();

        int exitCode = QaipCliApplication.runWithRuntime(
                new String[]{"import", temporaryDirectory.resolve("missing.json").toString()},
                streams.out, streams.err, () -> composition);

        assertEquals(CliExitCode.APPLICATION_FAILURE.value(), exitCode);
        assertEquals(0, calls.get());
        assertEquals("", streams.stdout());
        assertEquals("Import failed." + System.lineSeparator(), streams.stderr());
    }

    private static RuntimeComposition composition(ImportCliCommand command) {
        DataSource dataSource = (DataSource) Proxy.newProxyInstance(
                ImportDispatchTest.class.getClassLoader(), new Class<?>[]{DataSource.class},
                (proxy, method, args) -> null);
        return new RuntimeComposition(
                dataSource,
                project -> new ProjectInserted(project.metadata().id()),
                projectId -> Optional.empty(),
                command,
                new ImportTextRenderer());
    }
}
