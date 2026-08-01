package ru.kuznetsov.qaip.core.application.query.relationship;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.domain.Relationship;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProjectRelationshipLookupArchitectureTest {
    @Test
    void lookup_is_one_public_final_stateless_project_and_string_capability() throws Exception {
        assertTrue(Modifier.isPublic(ProjectRelationshipLookup.class.getModifiers()));
        assertTrue(Modifier.isFinal(ProjectRelationshipLookup.class.getModifiers()));
        assertEquals(0, ProjectRelationshipLookup.class.getDeclaredFields().length);
        assertEquals(1, ProjectRelationshipLookup.class.getDeclaredMethods().length);
        assertEquals(ProjectRelationships.class,
                ProjectRelationshipLookup.class.getMethod("findByNodeId", Project.class, String.class).getReturnType());
    }

    @Test
    void result_is_an_immutable_two_list_record_of_domain_relationships() {
        assertTrue(ProjectRelationships.class.isRecord());
        assertEquals(List.of("incoming", "outgoing"), java.util.Arrays.stream(
                ProjectRelationships.class.getRecordComponents()).map(component -> component.getName()).toList());
        assertTrue(java.util.Arrays.stream(ProjectRelationships.class.getRecordComponents())
                .allMatch(component -> component.getGenericType().getTypeName()
                        .equals("java.util.List<" + Relationship.class.getName() + ">")));
    }

    @Test
    void package_has_only_domain_and_standard_library_dependencies_without_frameworks() throws Exception {
        try (var files = Files.walk(Path.of(
                "src/main/java/ru/kuznetsov/qaip/core/application/query/relationship"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                for (String forbidden : List.of("ProjectReader", "ProjectNodeLookup", "persistence", "postgresql",
                        "java.sql", "javax.sql", "com.fasterxml", "cli", "springframework", "jakarta.persistence",
                        "hibernate", "Graph", "Cache", "parallelStream", "Map<")) {
                    assertFalse(source.contains(forbidden), () -> file + " contains " + forbidden);
                }
            }
        }
    }
}
