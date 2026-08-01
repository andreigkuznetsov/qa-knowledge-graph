package ru.kuznetsov.qaip.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.kuznetsov.qaip.core.application.importproject.ImportProjectPersistenceFailed;
import ru.kuznetsov.qaip.core.application.importproject.ImportProjectUseCase;
import ru.kuznetsov.qaip.core.application.importproject.ImportProjectUseCaseResult;
import ru.kuznetsov.qaip.core.application.importproject.ImportResultMapper;
import ru.kuznetsov.qaip.core.application.importproject.result.ImportPersistenceFailedResult;
import ru.kuznetsov.qaip.core.application.importproject.result.ImportResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImportCliCommandTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void reads_complete_utf8_preserves_exact_text_and_invokes_pipeline_once() throws IOException {
        String exactText = "{\r\n  \"name\": \"Привет 🌍\"\n}\r\n";
        Path file = temporaryDirectory.resolve("project.json");
        Files.writeString(file, exactText, StandardCharsets.UTF_8);
        AtomicInteger useCaseCalls = new AtomicInteger();
        AtomicReference<String> received = new AtomicReference<>();
        ImportProjectUseCaseResult applicationResult =
                new ImportProjectPersistenceFailed("Project persistence failed.");
        ImportProjectUseCase useCase = source -> {
            useCaseCalls.incrementAndGet();
            received.set(source.value());
            return applicationResult;
        };

        ImportResult result = new ImportCliCommand(useCase, new ImportResultMapper()).execute(file);

        assertEquals(exactText, received.get());
        assertEquals(1, useCaseCalls.get());
        assertEquals(new ImportPersistenceFailedResult("Project persistence failed."), result);
    }

    @Test
    void empty_file_reaches_use_case_unchanged() throws IOException {
        Path file = temporaryDirectory.resolve("empty.json");
        Files.write(file, new byte[0]);
        AtomicReference<String> received = new AtomicReference<>();
        ImportProjectUseCase useCase = source -> {
            received.set(source.value());
            return new ImportProjectPersistenceFailed("safe");
        };

        new ImportCliCommand(useCase, new ImportResultMapper()).execute(file);

        assertEquals("", received.get());
    }

    @Test
    void null_dependencies_and_path_are_rejected() {
        ImportProjectUseCase useCase = ignored -> new ImportProjectPersistenceFailed("safe");
        assertThrows(NullPointerException.class, () -> new ImportCliCommand(null, new ImportResultMapper()));
        assertThrows(NullPointerException.class, () -> new ImportCliCommand(useCase, null));
        assertThrows(NullPointerException.class,
                () -> new ImportCliCommand(useCase, new ImportResultMapper()).execute(null));
    }

    @Test
    void missing_file_and_directory_are_delivery_failures_without_application_invocation() {
        AtomicInteger calls = new AtomicInteger();
        ImportProjectUseCase useCase = ignored -> {
            calls.incrementAndGet();
            return new ImportProjectPersistenceFailed("safe");
        };
        ImportCliCommand command = new ImportCliCommand(useCase, new ImportResultMapper());

        ImportFileReadException missing = assertThrows(ImportFileReadException.class,
                () -> command.execute(temporaryDirectory.resolve("missing.json")));
        ImportFileReadException directory = assertThrows(ImportFileReadException.class,
                () -> command.execute(temporaryDirectory));

        assertEquals("Cannot read import file.", missing.getMessage());
        assertNotNull(missing.getCause());
        assertEquals("Cannot read import file.", directory.getMessage());
        assertNotNull(directory.getCause());
        assertEquals(0, calls.get());
    }

    @Test
    void utf8_read_failure_preserves_cause_and_does_not_invoke_application() throws IOException {
        Path file = temporaryDirectory.resolve("invalid-utf8.json");
        byte[] invalidUtf8 = {(byte) 0xC3, (byte) 0x28};
        Files.write(file, invalidUtf8);
        AtomicInteger calls = new AtomicInteger();
        ImportCliCommand command = new ImportCliCommand(ignored -> {
            calls.incrementAndGet();
            return new ImportProjectPersistenceFailed("safe");
        }, new ImportResultMapper());

        ImportFileReadException failure = assertThrows(ImportFileReadException.class,
                () -> command.execute(file));

        assertInstanceOf(java.nio.charset.MalformedInputException.class, failure.getCause());
        assertArrayEquals(invalidUtf8, Files.readAllBytes(file));
        assertEquals(0, calls.get());
    }

    @Test
    void use_case_and_mapper_runtime_failures_propagate_unchanged() throws IOException {
        Path file = temporaryDirectory.resolve("project.json");
        Files.writeString(file, "{}", StandardCharsets.UTF_8);
        IllegalStateException applicationFailure = new IllegalStateException("application failure");

        IllegalStateException propagated = assertThrows(IllegalStateException.class,
                () -> new ImportCliCommand(ignored -> {
                    throw applicationFailure;
                }, new ImportResultMapper()).execute(file));
        assertSame(applicationFailure, propagated);

        assertThrows(NullPointerException.class,
                () -> new ImportCliCommand(ignored -> null, new ImportResultMapper()).execute(file));
    }
}
