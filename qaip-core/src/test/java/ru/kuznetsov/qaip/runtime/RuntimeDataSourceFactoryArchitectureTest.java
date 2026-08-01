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

class RuntimeDataSourceFactoryArchitectureTest {
    @Test
    void factory_is_final_stateless_and_has_one_public_factory_method() throws Exception {
        assertTrue(Modifier.isPublic(RuntimeDataSourceFactory.class.getModifiers()));
        assertTrue(Modifier.isFinal(RuntimeDataSourceFactory.class.getModifiers()));
        assertTrue(Modifier.isPrivate(RuntimeDataSourceFactory.class.getDeclaredConstructors()[0].getModifiers()));
        assertEquals(0, RuntimeDataSourceFactory.class.getDeclaredFields().length);
        assertEquals(List.of("create"), Arrays.stream(RuntimeDataSourceFactory.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .map(method -> method.getName()).toList());
        assertEquals(DataSource.class, RuntimeDataSourceFactory.class.getMethod("create").getReturnType());
    }

    @Test
    void factory_has_no_repository_reader_connection_sql_bootstrap_or_cli_wiring() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/ru/kuznetsov/qaip/runtime/RuntimeDataSourceFactory.java"));
        for (String forbidden : List.of(
                "ProjectRepository", "ProjectReader", "QaipCliApplication", "getConnection(",
                "java.sql", "execute", "CREATE TABLE", "qaip_projects")) {
            assertFalse(source.contains(forbidden), forbidden);
        }
    }
}
