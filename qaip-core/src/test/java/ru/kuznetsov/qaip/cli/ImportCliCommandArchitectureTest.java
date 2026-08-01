package ru.kuznetsov.qaip.cli;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.importproject.ImportProjectUseCase;
import ru.kuznetsov.qaip.core.application.importproject.ImportResultMapper;
import ru.kuznetsov.qaip.core.application.importproject.result.ImportResult;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportCliCommandArchitectureTest {
    @Test
    void command_is_final_has_only_frozen_collaborators_and_accepts_path_not_arguments() throws Exception {
        assertTrue(Modifier.isFinal(ImportCliCommand.class.getModifiers()));
        assertEquals(Set.of(ImportProjectUseCase.class, ImportResultMapper.class),
                Arrays.stream(ImportCliCommand.class.getDeclaredFields())
                        .filter(field -> !Modifier.isStatic(field.getModifiers()))
                        .map(field -> field.getType())
                        .collect(Collectors.toSet()));
        var execute = ImportCliCommand.class.getMethod("execute", Path.class);
        assertEquals(ImportResult.class, execute.getReturnType());
        assertFalse(Arrays.stream(ImportCliCommand.class.getDeclaredMethods())
                .flatMap(method -> Arrays.stream(method.getParameterTypes()))
                .anyMatch(type -> type == String[].class));
    }

    @Test
    void command_has_one_use_case_call_one_mapper_call_and_no_output_dispatch_runtime_or_persistence_logic()
            throws Exception {
        Path sourceFile = Path.of("src/main/java/ru/kuznetsov/qaip/cli/ImportCliCommand.java");
        String source = Files.readString(sourceFile);
        assertEquals(1, occurrences(source, "useCase.execute("));
        assertEquals(1, occurrences(source, "mapper.map("));
        for (String forbidden : List.of("PrintStream", "System.out", "System.err", "ExitCode",
                "QaipCliApplication", "RuntimeComposition", "Repository", "DataSource", "java.sql",
                "Postgre", "Renderer", "String[]")) {
            assertFalse(source.contains(forbidden), () -> sourceFile + " contains " + forbidden);
        }
    }

    private static int occurrences(String source, String value) {
        return (source.length() - source.replace(value, "").length()) / value.length();
    }
}
