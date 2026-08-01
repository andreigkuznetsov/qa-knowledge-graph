package ru.kuznetsov.qaip.core.application.query.trace;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.domain.Project;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TraceGraphArchitectureTest {
    @Test
    void builder_is_public_final_stateless_with_exactly_one_public_build_method() throws Exception {
        assertTrue(Modifier.isPublic(TraceGraphBuilder.class.getModifiers()));
        assertTrue(Modifier.isFinal(TraceGraphBuilder.class.getModifiers()));
        assertEquals(0, TraceGraphBuilder.class.getDeclaredFields().length);
        assertEquals(1, TraceGraphBuilder.class.getDeclaredMethods().length);
        assertEquals(TraceGraph.class,
                TraceGraphBuilder.class.getMethod("build", Project.class, String.class).getReturnType());
    }

    @Test
    void implementation_has_only_domain_and_standard_library_dependencies() throws Exception {
        Path packagePath = Path.of("src/main/java/ru/kuznetsov/qaip/core/application/query/trace");
        for (Path file : Files.list(packagePath).toList()) {
            String source = Files.readString(file);
            for (String forbidden : new String[]{"ProjectReader", "Repository", "persistence", "postgresql",
                    "cli", "springframework", "jakarta.persistence", "hibernate", "parallelStream"}) {
                assertFalse(source.contains(forbidden), () -> file + " contains " + forbidden);
            }
        }
    }
}
