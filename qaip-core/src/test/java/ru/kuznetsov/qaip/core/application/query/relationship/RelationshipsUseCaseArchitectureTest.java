package ru.kuznetsov.qaip.core.application.query.relationship;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.node.ProjectNodeLookup;
import ru.kuznetsov.qaip.core.persistence.read.ProjectReader;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RelationshipsUseCaseArchitectureTest {
    @Test
    void entry_point_and_sealed_result_hierarchy_have_the_exact_public_shape() throws Exception {
        assertEquals(1, RelationshipsUseCase.class.getDeclaredMethods().length);
        var execute = RelationshipsUseCase.class.getDeclaredMethod("execute", String.class, String.class);
        assertEquals(RelationshipsQueryResult.class, execute.getReturnType());
        assertEquals(List.of(String.class, String.class), List.of(execute.getParameterTypes()));
        assertTrue(RelationshipsQueryResult.class.isSealed());
        assertEquals(Set.of(RelationshipsFound.class, RelationshipsProjectNotFound.class,
                        RelationshipsNodeNotFound.class),
                Set.of(RelationshipsQueryResult.class.getPermittedSubclasses()));
    }

    @Test
    void implementation_is_final_with_only_final_injected_dependencies() {
        assertTrue(Modifier.isFinal(DefaultRelationshipsUseCase.class.getModifiers()));
        assertEquals(1, DefaultRelationshipsUseCase.class.getConstructors().length);
        assertEquals(List.of(ProjectReader.class, ProjectNodeLookup.class, ProjectRelationshipLookup.class,
                        RelationshipDetailsMapper.class),
                List.of(DefaultRelationshipsUseCase.class.getConstructors()[0].getParameterTypes()));
        assertTrue(Arrays.stream(DefaultRelationshipsUseCase.class.getDeclaredFields())
                .allMatch(field -> Modifier.isFinal(field.getModifiers())));
    }

    @Test
    void results_enforce_invariants_preserve_values_and_leak_no_domain_or_optional() {
        RelationshipDetailsResult details = new RelationshipDetailsResult(List.of(), List.of());
        RelationshipsFound found = new RelationshipsFound(" P ", " N ", details);
        assertSame(details, found.relationships());
        assertEquals(found, new RelationshipsFound(" P ", " N ", details));
        assertEquals(found.hashCode(), new RelationshipsFound(" P ", " N ", details).hashCode());
        assertThrows(NullPointerException.class, () -> new RelationshipsFound(null, "N", details));
        assertThrows(IllegalArgumentException.class, () -> new RelationshipsFound(" ", "N", details));
        assertThrows(NullPointerException.class, () -> new RelationshipsFound("P", null, details));
        assertThrows(IllegalArgumentException.class, () -> new RelationshipsFound("P", " ", details));
        assertThrows(NullPointerException.class, () -> new RelationshipsFound("P", "N", null));
        assertThrows(NullPointerException.class, () -> new RelationshipsProjectNotFound(null));
        assertThrows(IllegalArgumentException.class, () -> new RelationshipsProjectNotFound(""));
        assertThrows(NullPointerException.class, () -> new RelationshipsNodeNotFound("P", null));
        assertThrows(IllegalArgumentException.class, () -> new RelationshipsNodeNotFound("P", "\t"));

        for (Class<?> api : List.of(RelationshipsUseCase.class, RelationshipsQueryResult.class,
                RelationshipsFound.class, RelationshipsProjectNotFound.class, RelationshipsNodeNotFound.class)) {
            for (var method : api.getMethods()) {
                String signature = method.toGenericString();
                assertFalse(signature.contains("core.domain.Project"), signature);
                assertFalse(signature.contains("core.domain.Node"), signature);
                assertFalse(signature.contains("core.domain.Relationship"), signature);
                assertFalse(signature.contains(Optional.class.getName()), signature);
            }
        }
    }

    @Test
    void orchestration_source_has_no_adapter_delivery_mapping_or_relationship_scan_logic() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/ru/kuznetsov/qaip/core/application/query/relationship/DefaultRelationshipsUseCase.java"));
        for (String forbidden : List.of("InMemoryProjectReader", "PostgreSqlProjectReader", "ProjectRepository",
                "persistence.memory", "persistence.postgresql", "java.sql", "com.fasterxml", "Controller",
                "Renderer", "Cli", "relationships()", ".from()", ".to()", "new RelationshipDetails(",
                "parallelStream", "QueryBus", "Mediator")) {
            assertFalse(source.contains(forbidden), forbidden);
        }
    }
}
