package ru.kuznetsov.qaip.core.application.query.nodedetails;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.node.ProjectNodeLookup;
import ru.kuznetsov.qaip.core.persistence.read.ProjectReader;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NodeDetailsUseCaseArchitectureTest {
    @Test
    void entry_point_is_one_two_string_to_typed_result_capability() throws Exception {
        assertTrue(Modifier.isPublic(NodeDetailsUseCase.class.getModifiers()));
        assertEquals(1, NodeDetailsUseCase.class.getDeclaredMethods().length);
        var execute = NodeDetailsUseCase.class.getDeclaredMethod("execute", String.class, String.class);
        assertEquals(NodeDetailsQueryResult.class, execute.getReturnType());
        assertEquals(List.of(String.class, String.class), List.of(execute.getParameterTypes()));
    }

    @Test
    void implementation_is_final_and_injected_with_port_lookup_and_mapper() {
        assertTrue(Modifier.isPublic(DefaultNodeDetailsUseCase.class.getModifiers()));
        assertTrue(Modifier.isFinal(DefaultNodeDetailsUseCase.class.getModifiers()));
        assertEquals(1, DefaultNodeDetailsUseCase.class.getConstructors().length);
        assertEquals(List.of(ProjectReader.class, ProjectNodeLookup.class, NodeDetailsMapper.class),
                List.of(DefaultNodeDetailsUseCase.class.getConstructors()[0].getParameterTypes()));
        assertTrue(Arrays.stream(DefaultNodeDetailsUseCase.class.getDeclaredFields())
                .allMatch(field -> Modifier.isFinal(field.getModifiers())));
    }

    @Test
    void public_api_and_implementation_leak_no_domain_optional_adapter_or_delivery_types() throws Exception {
        for (Class<?> api : List.of(NodeDetailsUseCase.class, NodeDetailsQueryResult.class,
                NodeDetailsFound.class, NodeDetailsProjectNotFound.class, NodeDetailsNodeNotFound.class)) {
            for (var method : api.getMethods()) {
                String signature = method.toGenericString();
                assertFalse(signature.contains("core.domain.Project"), signature);
                assertFalse(signature.contains("core.domain.Node"), signature);
                assertFalse(signature.contains("Optional"), signature);
            }
        }
        String source = Files.readString(Path.of(
                "src/main/java/ru/kuznetsov/qaip/core/application/query/nodedetails/DefaultNodeDetailsUseCase.java"));
        for (String forbidden : List.of("InMemoryProjectReader", "PostgreSqlProjectReader", "ProjectRepository",
                "persistence.memory", "persistence.postgresql", "persistence.document", "java.sql", "javax.sql",
                "com.fasterxml", "Controller", "Rest", "Cli", "QueryBus", "Mediator")) {
            assertFalse(source.contains(forbidden), forbidden);
        }
    }
}
