package ru.kuznetsov.qaip.core.application.query.node;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.domain.Node;
import ru.kuznetsov.qaip.core.domain.Project;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ProjectNodeLookupArchitectureTest {
    @Test
    void lookup_is_one_public_final_project_and_string_to_optional_node_capability() throws Exception {
        assertTrue(Modifier.isPublic(ProjectNodeLookup.class.getModifiers()));
        assertTrue(Modifier.isFinal(ProjectNodeLookup.class.getModifiers()));
        assertEquals(0, ProjectNodeLookup.class.getDeclaredFields().length);
        assertEquals(1, ProjectNodeLookup.class.getDeclaredMethods().length);
        var method = ProjectNodeLookup.class.getDeclaredMethod("findById", Project.class, String.class);
        assertTrue(Modifier.isPublic(method.getModifiers()));
        assertEquals(Optional.class, method.getReturnType());
        assertEquals("java.util.Optional<" + Node.class.getName() + ">", method.getGenericReturnType().getTypeName());
    }

    @Test
    void package_depends_only_on_domain_and_java_without_requirement_specific_or_generic_framework_code()
            throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/ru/kuznetsov/qaip/core/application/query/node/ProjectNodeLookup.java"));
        for (String forbidden : List.of("ProjectReader", "ProjectRepository", "persistence", "postgresql",
                "java.sql", "javax.sql", "com.fasterxml", "importing", "validation", "cli",
                "projectsummary", "springframework", "jakarta.persistence", "hibernate", "Requirement",
                "Predicate", "Strategy", "Cache", "Map<")) {
            assertFalse(source.contains(forbidden), forbidden);
        }
        assertFalse(Files.exists(Path.of(
                "src/main/java/ru/kuznetsov/qaip/core/application/query/node/DefaultProjectNodeLookup.java")));
    }
}
