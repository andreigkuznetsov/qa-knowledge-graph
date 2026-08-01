package ru.kuznetsov.qaip.core.application.importing;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.importproject.DefaultImportProjectUseCase;
import ru.kuznetsov.qaip.core.application.importproject.ImportProjectUseCase;
import ru.kuznetsov.qaip.core.application.importproject.ImportProjectUseCaseResult;
import ru.kuznetsov.qaip.core.application.persistence.PersistProject;
import ru.kuznetsov.qaip.core.importing.parsing.RawProjectJson;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportProjectUseCaseArchitectureTest {
    @Test
    void use_case_contract_accepts_only_raw_project_json() throws NoSuchMethodException {
        var method = ImportProjectUseCase.class.getMethod("execute", RawProjectJson.class);
        assertEquals(ImportProjectUseCaseResult.class, method.getReturnType());
        assertEquals(1, ImportProjectUseCase.class.getDeclaredMethods().length);
    }

    @Test
    void implementation_has_only_importer_and_persistence_collaborators() {
        Set<Class<?>> fields = Arrays.stream(DefaultImportProjectUseCase.class.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .map(field -> field.getType())
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of(ProjectImporter.class, PersistProject.class), fields);
    }

    @Test
    void production_slice_has_no_delivery_filesystem_runtime_jdbc_or_renderer_dependency() throws Exception {
        Path packagePath = Path.of("src/main/java/ru/kuznetsov/qaip/core/application/importproject");
        try (var files = Files.list(packagePath)) {
            for (Path file : files.filter(path -> path.getFileName().toString()
                    .startsWith("ImportProject") || path.getFileName().toString()
                    .equals("DefaultImportProjectUseCase.java")).toList()) {
                String source = Files.readString(file);
                assertFalse(source.contains("ru.kuznetsov.qaip.cli"));
                assertFalse(source.contains("ru.kuznetsov.qaip.runtime"));
                assertFalse(source.contains("java.io"));
                assertFalse(source.contains("java.nio.file"));
                assertFalse(source.contains("java.sql"));
                assertFalse(source.contains("Renderer"));
            }
        }
        assertTrue(Files.isRegularFile(packagePath.resolve("ImportProjectUseCase.java")));
    }
}
