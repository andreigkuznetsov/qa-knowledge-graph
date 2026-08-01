package ru.kuznetsov.qaip.core.persistence.postgresql;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.persistence.ProjectInsertResult;
import ru.kuznetsov.qaip.core.persistence.ProjectRepository;

import javax.sql.DataSource;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostgreSqlPersistenceArchitectureTest {
    @Test
    void adapter_is_public_final_and_implements_only_the_frozen_create_port() throws Exception {
        assertTrue(Modifier.isPublic(PostgreSqlProjectRepository.class.getModifiers()));
        assertTrue(Modifier.isFinal(PostgreSqlProjectRepository.class.getModifiers()));
        assertTrue(ProjectRepository.class.isAssignableFrom(PostgreSqlProjectRepository.class));
        assertEquals(ProjectInsertResult.class,
                PostgreSqlProjectRepository.class.getMethod("insertIfAbsent", Project.class).getReturnType());
        assertEquals(List.of("insertIfAbsent"), Arrays.stream(PostgreSqlProjectRepository.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .map(method -> method.getName()).distinct().toList());
        assertEquals(1, PostgreSqlProjectRepository.class.getConstructors().length);
        assertEquals(DataSource.class, PostgreSqlProjectRepository.class.getConstructors()[0].getParameterTypes()[0]);
    }

    @Test
    void public_repository_port_leaks_no_jdbc_postgresql_codec_or_document_types() {
        for (var method : ProjectRepository.class.getMethods()) {
            String signature = method.toGenericString();
            for (String forbidden : List.of("java.sql", "javax.sql", "org.postgresql",
                    "persistence.document", "persistence.postgresql")) {
                assertFalse(signature.contains(forbidden), signature);
            }
        }
    }

    @Test
    void postgresql_adapter_has_only_allowed_dependencies_and_frozen_layers_remain_independent() throws IOException {
        assertSourceExcludes("src/main/java/ru/kuznetsov/qaip/core/persistence/postgresql",
                "core.importing", "core.validation", "core.application", "com.fasterxml.jackson",
                "org.springframework", "jakarta.persistence", "org.hibernate", "org.jooq", "org.jdbi");
        for (String frozen : List.of("domain", "importing", "validation", "application")) {
            assertSourceExcludes("src/main/java/ru/kuznetsov/qaip/core/" + frozen,
                    "persistence.postgresql", "PostgreSqlProjectRepository");
        }
    }

    @Test
    void insert_sql_is_one_atomic_create_statement_without_read_update_or_replace() {
        String sql = PostgreSqlProjectRepository.INSERT_SQL.toUpperCase(java.util.Locale.ROOT);
        assertTrue(sql.contains("INSERT INTO QAIP_PROJECTS"));
        assertTrue(sql.contains("CAST(? AS JSONB)"));
        assertTrue(sql.contains("ON CONFLICT (PROJECT_ID) DO NOTHING"));
        assertFalse(sql.contains("SELECT"));
        assertFalse(sql.contains("UPDATE"));
        assertFalse(sql.contains("DELETE"));
    }

    private static void assertSourceExcludes(String root, String... forbidden) throws IOException {
        try (var files = Files.walk(Path.of(root))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                for (String value : forbidden) {
                    assertFalse(source.contains(value), () -> file + " contains " + value);
                }
            }
        }
    }
}
