package ru.kuznetsov.qaip.core.persistence.postgresql;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.domain.DeclaredChange;
import ru.kuznetsov.qaip.core.domain.EvidenceManifest;
import ru.kuznetsov.qaip.core.domain.Metadata;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.domain.Subject;
import ru.kuznetsov.qaip.core.persistence.ProjectAlreadyExists;
import ru.kuznetsov.qaip.core.persistence.ProjectInserted;
import ru.kuznetsov.qaip.core.persistence.ProjectPersistenceException;
import ru.kuznetsov.qaip.core.persistence.document.ProjectPersistenceDocumentCodec;
import ru.kuznetsov.qaip.core.persistence.document.ProjectPersistenceDocumentException;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostgreSqlProjectRepositoryTest {
    @Test
    void dependencies_and_project_are_non_null() {
        assertThrows(NullPointerException.class, () -> new PostgreSqlProjectRepository(null));
        assertThrows(NullPointerException.class, () -> new PostgreSqlProjectRepository(new StubDataSource(), null));
        assertThrows(NullPointerException.class,
                () -> new PostgreSqlProjectRepository(new StubDataSource(), codec("payload")).insertIfAbsent(null));
    }

    @Test
    void invalid_identity_fails_before_codec_or_data_source_access() {
        AtomicInteger codecCalls = new AtomicInteger();
        AtomicInteger connectionCalls = new AtomicInteger();
        StubDataSource dataSource = new StubDataSource();
        dataSource.calls = connectionCalls;
        ProjectPersistenceDocumentCodec codec = codecCalls(codecCalls, "payload");
        PostgreSqlProjectRepository repository = new PostgreSqlProjectRepository(dataSource, codec);

        Project missingMetadata = project(null, true);
        Project nullId = project(null, false);
        assertThrows(ProjectPersistenceException.class, () -> repository.insertIfAbsent(missingMetadata));
        assertThrows(ProjectPersistenceException.class, () -> repository.insertIfAbsent(nullId));
        assertThrows(ProjectPersistenceException.class, () -> repository.insertIfAbsent(project(" ", false)));
        assertEquals(0, codecCalls.get());
        assertEquals(0, connectionCalls.get());
    }

    @Test
    void codec_is_called_once_and_exact_id_payload_and_sql_are_bound() {
        JdbcScenario jdbc = new JdbcScenario(1);
        AtomicInteger codecCalls = new AtomicInteger();
        Project project = project(" exact-ID ", false);
        PostgreSqlProjectRepository repository = new PostgreSqlProjectRepository(
                jdbc.dataSource(), codecCalls(codecCalls, "exact-payload"));

        ProjectInserted result = assertInstanceOf(ProjectInserted.class, repository.insertIfAbsent(project));

        assertEquals(" exact-ID ", result.projectId());
        assertEquals(1, codecCalls.get());
        assertEquals(PostgreSqlProjectRepository.INSERT_SQL, jdbc.sql.get());
        assertEquals(" exact-ID ", jdbc.parameters.get().get(1));
        assertEquals("exact-payload", jdbc.parameters.get().get(2));
        assertTrue(jdbc.statementClosed.get());
        assertTrue(jdbc.connectionClosed.get());
    }

    @Test
    void zero_update_count_is_expected_duplicate_and_resources_close() {
        JdbcScenario jdbc = new JdbcScenario(0);
        ProjectAlreadyExists result = assertInstanceOf(ProjectAlreadyExists.class,
                new PostgreSqlProjectRepository(jdbc.dataSource(), codec("payload"))
                        .insertIfAbsent(project("P-1", false)));
        assertEquals("P-1", result.projectId());
        assertTrue(jdbc.statementClosed.get());
        assertTrue(jdbc.connectionClosed.get());
    }

    @Test
    void unexpected_update_count_is_contract_failure_and_resources_close() {
        JdbcScenario jdbc = new JdbcScenario(2);
        ProjectPersistenceException failure = assertThrows(ProjectPersistenceException.class,
                () -> new PostgreSqlProjectRepository(jdbc.dataSource(), codec("payload"))
                        .insertIfAbsent(project("P-1", false)));
        assertTrue(failure.getMessage().contains("Unexpected insert update count"));
        assertTrue(jdbc.statementClosed.get());
        assertTrue(jdbc.connectionClosed.get());
    }

    @Test
    void codec_failure_is_wrapped_with_cause_and_prevents_connection_access() {
        ProjectPersistenceDocumentException cause = new ProjectPersistenceDocumentException("codec failed");
        StubDataSource dataSource = new StubDataSource();
        AtomicInteger connections = new AtomicInteger();
        dataSource.calls = connections;
        ProjectPersistenceDocumentCodec codec = new ProjectPersistenceDocumentCodec() {
            @Override public String encode(Project project) { throw cause; }
            @Override public Project decode(String payload) { throw new UnsupportedOperationException(); }
        };
        ProjectPersistenceException failure = assertThrows(ProjectPersistenceException.class,
                () -> new PostgreSqlProjectRepository(dataSource, codec).insertIfAbsent(project("P-1", false)));
        assertSame(cause, failure.getCause());
        assertEquals(0, connections.get());
        assertFalse(failure.getMessage().contains("codec failed"));
    }

    @Test
    void connection_preparation_and_execution_failures_are_wrapped_and_resources_close() {
        SQLException connectionFailure = new SQLException("connection");
        StubDataSource failingDataSource = new StubDataSource();
        failingDataSource.failure = connectionFailure;
        assertSame(connectionFailure, assertThrows(ProjectPersistenceException.class,
                () -> new PostgreSqlProjectRepository(failingDataSource, codec("payload"))
                        .insertIfAbsent(project("P-1", false))).getCause());

        JdbcScenario preparation = new JdbcScenario(1);
        preparation.prepareFailure = new SQLException("prepare");
        assertSame(preparation.prepareFailure, assertThrows(ProjectPersistenceException.class,
                () -> new PostgreSqlProjectRepository(preparation.dataSource(), codec("payload"))
                        .insertIfAbsent(project("P-1", false))).getCause());
        assertTrue(preparation.connectionClosed.get());

        JdbcScenario execution = new JdbcScenario(1);
        execution.executeFailure = new SQLException("execute");
        assertSame(execution.executeFailure, assertThrows(ProjectPersistenceException.class,
                () -> new PostgreSqlProjectRepository(execution.dataSource(), codec("payload"))
                        .insertIfAbsent(project("P-1", false))).getCause());
        assertTrue(execution.statementClosed.get());
        assertTrue(execution.connectionClosed.get());
    }

    private static ProjectPersistenceDocumentCodec codec(String payload) {
        return codecCalls(new AtomicInteger(), payload);
    }

    private static ProjectPersistenceDocumentCodec codecCalls(AtomicInteger calls, String payload) {
        return new ProjectPersistenceDocumentCodec() {
            @Override public String encode(Project project) { calls.incrementAndGet(); return payload; }
            @Override public Project decode(String value) { throw new UnsupportedOperationException(); }
        };
    }

    private static Project project(String id, boolean omitMetadata) {
        Metadata metadata = omitMetadata ? null : new Metadata(id, "Project", null, null, Map.of());
        return new Project("qaip-project-v1", "0.1", metadata, List.of(), new Subject("local"),
                List.of(), List.of(), new EvidenceManifest("manifest", "source", Map.of(),
                "normalization", "canonicalization", "fingerprint", List.of(), List.of(), List.of()),
                List.of(new DeclaredChange("NODE", "N-1", "ADDED", "0.1", null, Map.of())), Map.of());
    }

    private static final class JdbcScenario {
        final int updateCount;
        final AtomicReference<String> sql = new AtomicReference<>();
        final AtomicReference<Map<Integer, String>> parameters = new AtomicReference<>(new java.util.HashMap<>());
        final AtomicBoolean connectionClosed = new AtomicBoolean();
        final AtomicBoolean statementClosed = new AtomicBoolean();
        SQLException prepareFailure;
        SQLException executeFailure;

        JdbcScenario(int updateCount) { this.updateCount = updateCount; }

        DataSource dataSource() {
            StubDataSource dataSource = new StubDataSource();
            dataSource.connection = connection();
            return dataSource;
        }

        private Connection connection() {
            return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Connection.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "prepareStatement" -> {
                            if (prepareFailure != null) throw prepareFailure;
                            sql.set((String) args[0]);
                            yield statement();
                        }
                        case "close" -> { connectionClosed.set(true); yield null; }
                        case "isClosed" -> connectionClosed.get();
                        case "unwrap" -> throw new SQLException("not a wrapper");
                        case "isWrapperFor" -> false;
                        default -> defaultValue(method.getReturnType());
                    });
        }

        private PreparedStatement statement() {
            return (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[] {PreparedStatement.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "setString" -> { parameters.get().put((Integer) args[0], (String) args[1]); yield null; }
                        case "executeUpdate" -> {
                            if (executeFailure != null) throw executeFailure;
                            yield updateCount;
                        }
                        case "close" -> { statementClosed.set(true); yield null; }
                        case "isClosed" -> statementClosed.get();
                        case "unwrap" -> throw new SQLException("not a wrapper");
                        case "isWrapperFor" -> false;
                        default -> defaultValue(method.getReturnType());
                    });
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0D;
        if (type == float.class) return 0F;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        if (type == char.class) return (char) 0;
        return null;
    }

    private static final class StubDataSource implements DataSource {
        Connection connection;
        SQLException failure;
        AtomicInteger calls = new AtomicInteger();

        @Override public Connection getConnection() throws SQLException {
            calls.incrementAndGet();
            if (failure != null) throw failure;
            return connection;
        }
        @Override public Connection getConnection(String username, String password) throws SQLException { return getConnection(); }
        @Override public PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(PrintWriter out) { }
        @Override public void setLoginTimeout(int seconds) { }
        @Override public int getLoginTimeout() { return 0; }
        @Override public Logger getParentLogger() throws SQLFeatureNotSupportedException { throw new SQLFeatureNotSupportedException(); }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { throw new SQLException("not a wrapper"); }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
    }
}
