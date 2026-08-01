package ru.kuznetsov.qaip.cli;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportDispatchArchitectureTest {
    @Test
    void centralized_dispatch_only_resolves_runtime_command_path_and_renders_its_result() throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/ru/kuznetsov/qaip/cli/QaipCliApplication.java"));
        assertEquals(1, occurrences(source, "composition.importCommand().execute("));
        assertEquals(1, occurrences(source, "composition.importRenderer().render("));
        assertTrue(source.contains("Path.of(file)"));
        for (String forbidden : List.of("new ImportCliCommand", "new ImportTextRenderer",
                "new DefaultImportProjectUseCase", "new ImportResultMapper", "RawProjectJson",
                "DefaultProjectImporter", "DefaultPersistProject")) {
            assertFalse(source.contains(forbidden), forbidden);
        }
    }

    private static int occurrences(String source, String value) {
        return (source.length() - source.replace(value, "").length()) / value.length();
    }
}
