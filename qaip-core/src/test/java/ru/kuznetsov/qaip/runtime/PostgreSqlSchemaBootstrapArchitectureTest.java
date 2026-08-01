package ru.kuznetsov.qaip.runtime;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostgreSqlSchemaBootstrapArchitectureTest {
    @Test
    void bootstrap_is_public_final_stateless_and_exposes_one_public_initialize_method() throws Exception {
        assertTrue(Modifier.isPublic(PostgreSqlSchemaBootstrap.class.getModifiers()));
        assertTrue(Modifier.isFinal(PostgreSqlSchemaBootstrap.class.getModifiers()));
        assertEquals(1, PostgreSqlSchemaBootstrap.class.getDeclaredFields().length);
        assertTrue(Modifier.isStatic(PostgreSqlSchemaBootstrap.class.getDeclaredFields()[0].getModifiers()));
        assertTrue(Modifier.isFinal(PostgreSqlSchemaBootstrap.class.getDeclaredFields()[0].getModifiers()));
        assertEquals(List.of("initialize"), Arrays.stream(PostgreSqlSchemaBootstrap.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .map(method -> method.getName()).toList());
        assertEquals(void.class,
                PostgreSqlSchemaBootstrap.class.getMethod("initialize", DataSource.class).getReturnType());
    }

    @Test
    void bootstrap_has_no_runtime_wiring_product_persistence_or_duplicated_ddl_dependencies() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/ru/kuznetsov/qaip/runtime/PostgreSqlSchemaBootstrap.java"));
        for (String forbidden : List.of("RuntimeComposition", "QaipCliApplication", "ProjectRepository",
                "ProjectReader", "core.application", "core.domain", "CREATE TABLE", "IF NOT EXISTS")) {
            assertFalse(source.contains(forbidden), forbidden);
        }
        assertTrue(source.contains("postgresql/qaip-projects.sql"));
    }
}
