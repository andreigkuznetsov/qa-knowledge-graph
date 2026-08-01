package ru.kuznetsov.qaip.core.application.query.relationship;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RelationshipDetailsArchitectureTest {
    @Test
    void mapper_is_one_public_final_stateless_project_relationships_to_result_operation() throws Exception {
        assertTrue(Modifier.isPublic(RelationshipDetailsMapper.class.getModifiers()));
        assertTrue(Modifier.isFinal(RelationshipDetailsMapper.class.getModifiers()));
        assertEquals(0, RelationshipDetailsMapper.class.getDeclaredFields().length);
        assertEquals(RelationshipDetailsResult.class,
                RelationshipDetailsMapper.class.getMethod("map", ProjectRelationships.class).getReturnType());
        assertEquals(List.of("map"), java.util.Arrays.stream(RelationshipDetailsMapper.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers())).map(method -> method.getName()).toList());
    }

    @Test
    void item_result_has_exactly_four_scalar_strings_and_aggregate_leaks_no_domain_relationship() {
        assertTrue(RelationshipDetails.class.isRecord());
        assertEquals(List.of("relationshipId", "fromNodeId", "toNodeId", "relationshipType"),
                java.util.Arrays.stream(RelationshipDetails.class.getRecordComponents())
                        .map(component -> component.getName()).toList());
        assertTrue(java.util.Arrays.stream(RelationshipDetails.class.getRecordComponents())
                .allMatch(component -> component.getType() == String.class));
        assertTrue(java.util.Arrays.stream(RelationshipDetailsResult.class.getRecordComponents())
                .allMatch(component -> component.getGenericType().getTypeName()
                        .equals("java.util.List<" + RelationshipDetails.class.getName() + ">")));
    }

    @Test
    void mapping_source_has_no_project_lookup_reader_persistence_cli_or_framework_dependency() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/ru/kuznetsov/qaip/core/application/query/relationship/RelationshipDetailsMapper.java"));
        for (String forbidden : List.of("core.domain.Project", "ProjectRelationshipLookup", "ProjectReader",
                "persistence", "postgresql", "java.sql", "javax.sql", "com.fasterxml", "cli",
                "springframework", "jakarta.persistence", "hibernate", "Graph", "sort(", "distinct(")) {
            assertFalse(source.contains(forbidden), forbidden);
        }
    }
}
