package ru.kuznetsov.qaip.core.application.query.trace;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.node.ProjectNodeLookup;
import ru.kuznetsov.qaip.core.persistence.read.ProjectReader;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TraceUseCaseArchitectureTest {
    @Test
    void hierarchy_is_sealed_with_exactly_three_record_subtypes_and_no_domain_leakage() {
        assertTrue(TraceQueryResult.class.isSealed());
        assertEquals(Set.of(TraceFound.class, TraceProjectNotFound.class, TraceNodeNotFound.class),
                Set.of(TraceQueryResult.class.getPermittedSubclasses()));
        for (Class<?> type : TraceQueryResult.class.getPermittedSubclasses()) assertTrue(type.isRecord());
        for (Class<?> type : List.of(TraceUseCase.class, TraceQueryResult.class, TraceFound.class,
                TraceProjectNotFound.class, TraceNodeNotFound.class)) {
            String signature = type.toGenericString() + Arrays.toString(type.getDeclaredMethods())
                    + Arrays.toString(type.getDeclaredFields());
            for (String forbidden : List.of("Project", "Node", "Relationship", "TraceGraph", "Optional")) {
                assertFalse(signature.contains("core.domain." + forbidden));
            }
        }
    }

    @Test
    void implementation_is_final_injected_and_exposes_one_public_execute_method() throws Exception {
        assertTrue(Modifier.isFinal(DefaultTraceUseCase.class.getModifiers()));
        assertEquals(4, DefaultTraceUseCase.class.getDeclaredFields().length);
        assertTrue(Arrays.stream(DefaultTraceUseCase.class.getDeclaredFields())
                .allMatch(field -> Modifier.isFinal(field.getModifiers())));
        assertEquals(List.of(ProjectReader.class, ProjectNodeLookup.class, TraceGraphBuilder.class, TraceMapper.class),
                List.of(DefaultTraceUseCase.class.getConstructors()[0].getParameterTypes()));
        assertEquals(1, Arrays.stream(DefaultTraceUseCase.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers())).count());
        assertEquals(TraceQueryResult.class,
                DefaultTraceUseCase.class.getMethod("execute", String.class, String.class).getReturnType());
    }

    @Test
    void source_is_orchestration_only_without_adapters_traversal_mapping_or_delivery() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/ru/kuznetsov/qaip/core/application/query/trace/DefaultTraceUseCase.java"));
        for (String forbidden : List.of("InMemory", "PostgreSql", "Repository", "new TraceGraphBuilder",
                "new TraceMapper", "relationship.", ".nodes()", ".relationships()", "queue", "visited",
                "new TraceNode(", "new TraceRelationship(", "cli", "Renderer", "catch (")) {
            assertFalse(source.contains(forbidden), () -> "use case contains " + forbidden);
        }
    }
}
