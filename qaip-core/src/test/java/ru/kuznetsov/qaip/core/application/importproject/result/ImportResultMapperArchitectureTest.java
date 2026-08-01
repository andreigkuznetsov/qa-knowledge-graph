package ru.kuznetsov.qaip.core.application.importproject.result;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.importproject.ImportProjectUseCaseResult;
import ru.kuznetsov.qaip.core.application.importproject.ImportResultMapper;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportResultMapperArchitectureTest {
    @Test
    void mapper_is_final_stateless_and_exposes_exactly_one_public_map_method() throws Exception {
        assertTrue(Modifier.isFinal(ImportResultMapper.class.getModifiers()));
        assertEquals(0, ImportResultMapper.class.getDeclaredFields().length);
        var publicMethods = Arrays.stream(ImportResultMapper.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .toList();
        assertEquals(1, publicMethods.size());
        assertEquals("map", publicMethods.getFirst().getName());
        assertEquals(List.of(ImportProjectUseCaseResult.class), List.of(publicMethods.getFirst().getParameterTypes()));
        assertEquals(ImportResult.class, publicMethods.getFirst().getReturnType());
    }

    @Test
    void mapper_source_has_only_result_mapping_dependencies_and_no_delivery_or_orchestration_logic() throws Exception {
        Path sourceFile = Path.of(
                "src/main/java/ru/kuznetsov/qaip/core/application/importproject/ImportResultMapper.java");
        String source = Files.readString(sourceFile);
        for (String forbidden : List.of("ProjectImporter", "PersistProject", "ImportProjectUseCase;",
                "qaip.cli", "qaip.runtime", "java.io", "java.nio.file", "java.sql", "Repository",
                "Renderer", "ExitCode", "Postgre")) {
            assertFalse(source.contains(forbidden), () -> sourceFile + " contains " + forbidden);
        }
    }
}
