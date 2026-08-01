package ru.kuznetsov.qaip.core.application.query.projectsummary;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.domain.Project;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProjectSummaryArchitectureTest {
    @Test
    void result_is_a_public_record_with_exactly_eight_scalar_components() {
        assertTrue(Modifier.isPublic(ProjectSummaryResult.class.getModifiers()));
        assertTrue(ProjectSummaryResult.class.isRecord());
        var components = ProjectSummaryResult.class.getRecordComponents();
        assertEquals(8, components.length);
        for (var component : components) {
            assertTrue(component.getType() == String.class || component.getType() == int.class,
                    component.toString());
        }
    }

    @Test
    void mapper_is_one_public_final_stateless_project_to_result_operation() throws Exception {
        assertTrue(Modifier.isPublic(ProjectSummaryMapper.class.getModifiers()));
        assertTrue(Modifier.isFinal(ProjectSummaryMapper.class.getModifiers()));
        assertEquals(0, ProjectSummaryMapper.class.getDeclaredFields().length);
        assertEquals(ProjectSummaryResult.class,
                ProjectSummaryMapper.class.getMethod("map", Project.class).getReturnType());
        assertEquals(List.of("map"), java.util.Arrays.stream(ProjectSummaryMapper.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers())).map(method -> method.getName()).toList());
    }

    @Test
    void package_has_no_storage_delivery_validation_json_or_framework_dependencies() throws Exception {
        Path root = Path.of("src/main/java/ru/kuznetsov/qaip/core/application/query/projectsummary");
        for (String name : List.of("ProjectSummaryMapper.java", "ProjectSummaryResult.java")) {
            Path file = root.resolve(name);
            String source = Files.readString(file);
            for (String forbidden : List.of("ProjectReader", "ProjectRepository", "persistence", "postgresql",
                    "java.sql", "javax.sql", "com.fasterxml", "importing", "validation", "springframework",
                    "jakarta.persistence", "hibernate", "Controller", "UseCase", "Handler")) {
                assertFalse(source.contains(forbidden), () -> file + " contains " + forbidden);
            }
        }
    }
}
