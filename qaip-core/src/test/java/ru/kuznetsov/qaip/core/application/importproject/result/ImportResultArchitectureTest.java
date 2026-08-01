package ru.kuznetsov.qaip.core.application.importproject.result;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ImportResultArchitectureTest {
    @Test
    void sealed_hierarchy_has_exactly_four_subtypes() {
        assertEquals(Set.of(ImportCompletedResult.class, ImportRejectedResult.class,
                        ImportPersistenceRejectedResult.class, ImportPersistenceFailedResult.class),
                Set.of(ImportResult.class.getPermittedSubclasses()));
    }

    @Test
    void dto_fields_expose_only_scalars_and_the_finding_list() {
        assertComponents(ImportCompletedResult.class, String.class, int.class, int.class);
        assertComponents(ImportRejectedResult.class, String.class, List.class);
        assertComponents(ImportPersistenceRejectedResult.class, String.class, String.class);
        assertComponents(ImportPersistenceFailedResult.class, String.class);
        assertComponents(ImportFindingResult.class, String.class, String.class, String.class, String.class);
    }

    @Test
    void slice_has_no_mapper_delivery_runtime_filesystem_jdbc_repository_or_renderer_dependency() throws Exception {
        Path root = Path.of("src/main/java/ru/kuznetsov/qaip/core/application/importproject/result");
        try (var files = Files.list(root)) {
            List<Path> sources = files.filter(path -> path.toString().endsWith(".java")).toList();
            assertEquals(6, sources.size());
            for (Path sourceFile : sources) {
                String source = Files.readString(sourceFile);
                for (String forbidden : List.of("Mapper", "qaip.cli", "qaip.runtime", "java.io",
                        "java.nio.file", "java.sql", "Repository", "Renderer", "application.importing",
                        "application.persistence", "core.persistence")) {
                    assertFalse(source.contains(forbidden), () -> sourceFile + " contains " + forbidden);
                }
            }
        }
    }

    private static void assertComponents(Class<?> type, Class<?>... expected) {
        assertEquals(List.of(expected), Arrays.stream(type.getRecordComponents())
                .map(RecordComponent::getType)
                .collect(Collectors.toList()));
    }
}
