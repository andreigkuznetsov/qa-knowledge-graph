package ru.kuznetsov.qaip.core.application.query.nodedetails;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.domain.Node;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NodeDetailsArchitectureTest {
    @Test
    void mapper_is_one_public_final_stateless_node_to_result_operation() throws Exception {
        assertTrue(Modifier.isPublic(NodeDetailsMapper.class.getModifiers()));
        assertTrue(Modifier.isFinal(NodeDetailsMapper.class.getModifiers()));
        assertEquals(0, NodeDetailsMapper.class.getDeclaredFields().length);
        assertEquals(1, NodeDetailsMapper.class.getDeclaredMethods().length);
        assertEquals(NodeDetailsResult.class, NodeDetailsMapper.class.getMethod("map", Node.class).getReturnType());
        assertFalse(Files.exists(Path.of(
                "src/main/java/ru/kuznetsov/qaip/core/application/query/nodedetails/DefaultNodeDetailsMapper.java")));
    }

    @Test
    void result_is_a_public_record_with_exactly_five_string_components() {
        assertTrue(Modifier.isPublic(NodeDetailsResult.class.getModifiers()));
        assertTrue(NodeDetailsResult.class.isRecord());
        assertEquals(List.of("nodeId", "nodeType", "name", "description", "status"),
                java.util.Arrays.stream(NodeDetailsResult.class.getRecordComponents())
                        .map(component -> component.getName()).toList());
        assertTrue(java.util.Arrays.stream(NodeDetailsResult.class.getRecordComponents())
                .allMatch(component -> component.getType() == String.class));
    }

    @Test
    void package_has_only_domain_node_and_standard_library_dependencies() throws Exception {
        try (var files = Files.walk(Path.of(
                "src/main/java/ru/kuznetsov/qaip/core/application/query/nodedetails"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                for (String forbidden : List.of("core.domain.Project", "ProjectNodeLookup", "ProjectReader",
                        "persistence", "postgresql", "java.sql", "javax.sql", "com.fasterxml", "importing",
                        "validation", "cli", "projectsummary", "springframework", "jakarta.persistence",
                        "hibernate", "Requirement", "relationship", "evidence", "Map<", "List<", "Optional<",
                        "UseCase")) {
                    assertFalse(source.contains(forbidden), () -> file + " contains " + forbidden);
                }
            }
        }
    }
}
