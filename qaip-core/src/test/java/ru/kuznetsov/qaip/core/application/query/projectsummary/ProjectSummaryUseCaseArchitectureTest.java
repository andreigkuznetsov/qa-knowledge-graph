package ru.kuznetsov.qaip.core.application.query.projectsummary;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.persistence.read.ProjectReader;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProjectSummaryUseCaseArchitectureTest {
    @Test
    void entry_point_is_one_string_to_typed_result_capability() throws Exception {
        assertTrue(Modifier.isPublic(ProjectSummaryUseCase.class.getModifiers()));
        assertEquals(1, ProjectSummaryUseCase.class.getDeclaredMethods().length);
        var execute = ProjectSummaryUseCase.class.getDeclaredMethod("execute", String.class);
        assertEquals(ProjectSummaryQueryResult.class, execute.getReturnType());
        assertEquals(List.of(String.class), List.of(execute.getParameterTypes()));
    }

    @Test
    void implementation_is_public_final_and_injected_with_port_and_mapper() {
        assertTrue(Modifier.isPublic(DefaultProjectSummaryUseCase.class.getModifiers()));
        assertTrue(Modifier.isFinal(DefaultProjectSummaryUseCase.class.getModifiers()));
        assertEquals(1, DefaultProjectSummaryUseCase.class.getConstructors().length);
        assertEquals(List.of(ProjectReader.class, ProjectSummaryMapper.class),
                List.of(DefaultProjectSummaryUseCase.class.getConstructors()[0].getParameterTypes()));
        assertTrue(Arrays.stream(DefaultProjectSummaryUseCase.class.getDeclaredFields())
                .allMatch(field -> Modifier.isFinal(field.getModifiers())));
    }

    @Test
    void use_case_has_no_adapter_storage_delivery_or_infrastructure_dependency() throws Exception {
        for (Class<?> api : List.of(ProjectSummaryUseCase.class, ProjectSummaryQueryResult.class,
                ProjectSummaryFound.class, ProjectSummaryNotFound.class)) {
            for (var method : api.getMethods()) {
                String signature = method.toGenericString();
                assertFalse(signature.contains("core.domain.Project"), signature);
                assertFalse(signature.contains("Optional"), signature);
            }
        }
        String source = Files.readString(Path.of(
                "src/main/java/ru/kuznetsov/qaip/core/application/query/projectsummary/DefaultProjectSummaryUseCase.java"));
        for (String forbidden : List.of("InMemoryProjectReader", "PostgreSqlProjectReader", "ProjectRepository",
                "persistence.memory", "persistence.postgresql", "persistence.document", "java.sql", "javax.sql",
                "com.fasterxml", "Controller", "Rest", "Cli", "QueryBus", "Mediator")) {
            assertFalse(source.contains(forbidden), forbidden);
        }
    }
}
