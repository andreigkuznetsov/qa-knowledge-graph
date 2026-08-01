package ru.kuznetsov.qaip.cli;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.importproject.result.ImportResult;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportTextRendererArchitectureTest {
    @Test
    void renderer_is_final_stateless_and_exposes_exactly_one_public_render_method() throws Exception {
        assertTrue(Modifier.isFinal(ImportTextRenderer.class.getModifiers()));
        assertEquals(0, ImportTextRenderer.class.getDeclaredFields().length);
        var publicMethods = Arrays.stream(ImportTextRenderer.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .toList();
        assertEquals(1, publicMethods.size());
        assertEquals("render", publicMethods.getFirst().getName());
        assertEquals(List.of(ImportResult.class), List.of(publicMethods.getFirst().getParameterTypes()));
        assertEquals(String.class, publicMethods.getFirst().getReturnType());
    }

    @Test
    void renderer_has_only_dto_and_standard_library_dependencies_and_no_delivery_execution_logic()
            throws Exception {
        Path sourceFile = Path.of("src/main/java/ru/kuznetsov/qaip/cli/ImportTextRenderer.java");
        String source = Files.readString(sourceFile);
        for (String forbidden : List.of("ImportCliCommand", "ImportProjectUseCase", "ImportResultMapper",
                "RawProjectJson", "java.io", "java.nio.file", "PrintStream", "System.out", "System.err",
                "ExitCode", "QaipCliApplication", "RuntimeComposition", "Repository", "DataSource",
                "java.sql", "Postgre")) {
            assertFalse(source.contains(forbidden), () -> sourceFile + " contains " + forbidden);
        }
    }
}
