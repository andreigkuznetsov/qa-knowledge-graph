package ru.kuznetsov.qaip.runtime;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostgreSqlSchemaBootstrapTest {
    @Test
    void loads_and_executes_the_exact_production_sql_once_and_closes_resources() throws Exception {
        String expected = resourceSql();
        JdbcScenario jdbc = new JdbcScenario();

        new PostgreSqlSchemaBootstrap().initialize(jdbc.dataSource());

        assertEquals(1, jdbc.connections.get());
        assertEquals(1, jdbc.statements.get());
        assertEquals(List.of(expected), jdbc.executedSql);
        assertEquals(1, jdbc.closedStatements.get());
        assertEquals(1, jdbc.closedConnections.get());
    }

    @Test
    void rejects_null_data_source_before_loading_the_resource() {
        AtomicInteger loads = new AtomicInteger();
        assertThrows(NullPointerException.class, () -> new PostgreSqlSchemaBootstrap().initialize(null, path -> {
            loads.incrementAndGet();
            return null;
        }));
        assertEquals(0, loads.get());
    }

    @Test
    void missing_and_unreadable_resources_fail_clearly_with_cause_when_available() {
        PostgreSqlSchemaBootstrap bootstrap = new PostgreSqlSchemaBootstrap();
        JdbcScenario jdbc = new JdbcScenario();

        IllegalStateException missing = assertThrows(IllegalStateException.class,
                () -> bootstrap.initialize(jdbc.dataSource(), path -> null));
        assertTrue(missing.getMessage().contains("postgresql/qaip-projects.sql"));

        IOException cause = new IOException("unreadable");
        InputStream unreadable = new InputStream() {
            @Override
            public int read() throws IOException {
                throw cause;
            }
        };
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> bootstrap.initialize(jdbc.dataSource(), path -> unreadable));
        assertTrue(failure.getMessage().contains("postgresql/qaip-projects.sql"));
        assertSame(cause, failure.getCause());
        assertEquals(0, jdbc.connections.get());
    }

    @Test
    void sql_failure_preserves_cause_and_closes_statement_and_connection() {
        JdbcScenario jdbc = new JdbcScenario();
        jdbc.executeFailure = new SQLException("credentials-must-not-be-repeated");

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> new PostgreSqlSchemaBootstrap().initialize(jdbc.dataSource()));

        assertEquals("Cannot initialize PostgreSQL schema", failure.getMessage());
        assertSame(jdbc.executeFailure, failure.getCause());
        assertEquals(1, jdbc.closedStatements.get());
        assertEquals(1, jdbc.closedConnections.get());
    }

    @Test
    void repeated_initialization_executes_once_per_call_without_java_state() {
        JdbcScenario jdbc = new JdbcScenario();
        PostgreSqlSchemaBootstrap bootstrap = new PostgreSqlSchemaBootstrap();

        bootstrap.initialize(jdbc.dataSource());
        bootstrap.initialize(jdbc.dataSource());

        assertEquals(2, jdbc.connections.get());
        assertEquals(2, jdbc.statements.get());
        assertEquals(2, jdbc.executedSql.size());
        assertEquals(2, jdbc.closedStatements.get());
        assertEquals(2, jdbc.closedConnections.get());
    }

    private static String resourceSql() throws Exception {
        try (var stream = PostgreSqlSchemaBootstrapTest.class.getResourceAsStream(
                "/postgresql/qaip-projects.sql")) {
            return new String(java.util.Objects.requireNonNull(stream).readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static final class JdbcScenario {
        final AtomicInteger connections = new AtomicInteger();
        final AtomicInteger statements = new AtomicInteger();
        final AtomicInteger closedConnections = new AtomicInteger();
        final AtomicInteger closedStatements = new AtomicInteger();
        final List<String> executedSql = new ArrayList<>();
        SQLException executeFailure;

        DataSource dataSource() {
            return (DataSource) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{DataSource.class}, (proxy, method, args) -> {
                        if (method.getName().equals("getConnection")) {
                            connections.incrementAndGet();
                            return connection();
                        }
                        return defaultValue(method.getReturnType());
                    });
        }

        private Connection connection() {
            return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{Connection.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "createStatement" -> {
                            statements.incrementAndGet();
                            yield statement();
                        }
                        case "close" -> {
                            closedConnections.incrementAndGet();
                            yield null;
                        }
                        default -> defaultValue(method.getReturnType());
                    });
        }

        private Statement statement() {
            return (Statement) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{Statement.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "execute" -> {
                            if (executeFailure != null) throw executeFailure;
                            executedSql.add((String) args[0]);
                            yield false;
                        }
                        case "close" -> {
                            closedStatements.incrementAndGet();
                            yield null;
                        }
                        default -> defaultValue(method.getReturnType());
                    });
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        return 0;
    }
}
