package ru.kuznetsov.qaip.core.application.query.trace;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TraceMappingArchitectureTest {
    @Test
    void mapper_is_public_final_stateless_with_one_public_map_method() throws Exception {
        assertTrue(Modifier.isPublic(TraceMapper.class.getModifiers()));
        assertTrue(Modifier.isFinal(TraceMapper.class.getModifiers()));
        assertEquals(0, TraceMapper.class.getDeclaredFields().length);
        assertEquals(1, Arrays.stream(TraceMapper.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers())).count());
        assertEquals(TraceResult.class, TraceMapper.class.getMethod("map", TraceGraph.class).getReturnType());
    }

    @Test
    void dto_records_have_only_the_required_scalar_and_dto_list_shapes() {
        assertComponents(TraceNode.class, List.of("nodeId", "nodeType"), List.of(String.class, String.class));
        assertComponents(TraceRelationship.class,
                List.of("relationshipId", "fromNodeId", "toNodeId", "relationshipType"),
                List.of(String.class, String.class, String.class, String.class));
        assertTrue(TraceResult.class.isRecord());
        assertEquals(List.of("startNodeId", "nodes", "relationships"), Arrays.stream(
                TraceResult.class.getRecordComponents()).map(component -> component.getName()).toList());
        assertEquals(String.class, TraceResult.class.getRecordComponents()[0].getType());
        assertListElement(TraceResult.class.getRecordComponents()[1].getGenericType(), TraceNode.class);
        assertListElement(TraceResult.class.getRecordComponents()[2].getGenericType(), TraceRelationship.class);
    }

    @Test
    void mapper_does_not_access_project_builder_or_graph_algorithms() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/ru/kuznetsov/qaip/core/application/query/trace/TraceMapper.java"));
        for (String forbidden : List.of("Project", "TraceGraphBuilder", "ProjectReader", "Repository",
                "queue", "visited", "adjacency", "sort", "filter", "distinct")) {
            assertFalse(source.contains(forbidden), () -> "TraceMapper contains " + forbidden);
        }
    }

    private static void assertComponents(Class<?> type, List<String> names, List<Class<?>> types) {
        assertTrue(type.isRecord());
        assertEquals(names, Arrays.stream(type.getRecordComponents()).map(component -> component.getName()).toList());
        assertEquals(types, Arrays.stream(type.getRecordComponents()).map(component -> component.getType()).toList());
    }

    private static void assertListElement(java.lang.reflect.Type type, Class<?> elementType) {
        assertInstanceOf(ParameterizedType.class, type);
        ParameterizedType parameterized = (ParameterizedType) type;
        assertEquals(List.class, parameterized.getRawType());
        assertEquals(elementType, parameterized.getActualTypeArguments()[0]);
    }
}
