package ru.kuznetsov.qaip.core.persistence.postgresql;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.domain.DeclaredChange;
import ru.kuznetsov.qaip.core.domain.EvidenceManifest;
import ru.kuznetsov.qaip.core.domain.Metadata;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.domain.Subject;
import ru.kuznetsov.qaip.core.persistence.ProjectPersistenceException;
import ru.kuznetsov.qaip.core.persistence.document.ProjectPersistenceDocumentCodec;
import ru.kuznetsov.qaip.core.persistence.document.ProjectPersistenceDocumentException;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PostgreSqlProjectReaderTest {
    @Test
    void reads_one_row_with_exact_sql_parameter_and_closes_resources() {
        Project project = project("P-1", "First");
        JdbcStub jdbc = new JdbcStub("payload");
        AtomicInteger decodes = new AtomicInteger();
        ProjectPersistenceDocumentCodec codec = codec(payload -> { decodes.incrementAndGet(); return project; });

        assertSame(project, new PostgreSqlProjectReader(jdbc, codec).findById("P-1").orElseThrow());
        assertEquals(PostgreSqlProjectReader.SELECT_SQL, jdbc.sql);
        assertEquals("P-1", jdbc.parameter);
        assertEquals(1, decodes.get());
        assertTrue(jdbc.resultClosed.get());
        assertTrue(jdbc.statementClosed.get());
        assertTrue(jdbc.connectionClosed.get());
    }

    @Test
    void absence_skips_codec_and_input_is_rejected_before_jdbc() {
        JdbcStub jdbc = new JdbcStub(null);
        AtomicInteger decodes = new AtomicInteger();
        var reader = new PostgreSqlProjectReader(jdbc, codec(payload -> { decodes.incrementAndGet(); return null; }));
        assertTrue(reader.findById("missing").isEmpty());
        assertEquals(0, decodes.get());
        int calls = jdbc.connections.get();
        assertThrows(NullPointerException.class, () -> reader.findById(null));
        assertThrows(IllegalArgumentException.class, () -> reader.findById(" "));
        assertEquals(calls, jdbc.connections.get());
        assertThrows(NullPointerException.class, () -> new PostgreSqlProjectReader(null));
        assertThrows(NullPointerException.class, () -> new PostgreSqlProjectReader(jdbc, null));
    }

    @Test
    void wraps_sql_and_codec_failures_and_rejects_identity_mismatch() {
        JdbcStub failed = new JdbcStub(null);
        failed.failure = new SQLException("offline");
        ProjectPersistenceException sql = assertThrows(ProjectPersistenceException.class,
                () -> new PostgreSqlProjectReader(failed).findById("P-1"));
        assertSame(failed.failure, sql.getCause());

        JdbcStub row = new JdbcStub("bad");
        ProjectPersistenceDocumentException decode = new ProjectPersistenceDocumentException("bad payload");
        ProjectPersistenceException codecFailure = assertThrows(ProjectPersistenceException.class,
                () -> new PostgreSqlProjectReader(row, codec(payload -> { throw decode; })).findById("P-1"));
        assertSame(decode, codecFailure.getCause());

        Project wrong = project("P-2", "Wrong");
        ProjectPersistenceException mismatch = assertThrows(ProjectPersistenceException.class,
                () -> new PostgreSqlProjectReader(new JdbcStub("payload"), codec(payload -> wrong)).findById("P-1"));
        assertTrue(mismatch.getMessage().contains("P-1"));
        assertTrue(mismatch.getMessage().contains("P-2"));
    }

    private interface Decoder { Project decode(String payload); }
    private static Project project(String id, String name) {
        return new Project("qaip-project-v1", "0.1", new Metadata(id, name, null, null, Map.of()),
                List.of(), new Subject("local"), List.of(), List.of(),
                new EvidenceManifest("impact-evidence-manifest-v1", "source", Map.of(), "normalization",
                        "canonicalization", "fingerprint", List.of(), List.of(), List.of()),
                List.of(new DeclaredChange("NODE", "N-1", "ADDED", "0.1", null, Map.of())), Map.of());
    }

    private static ProjectPersistenceDocumentCodec codec(Decoder decoder) {
        return new ProjectPersistenceDocumentCodec() {
            public String encode(Project project) { throw new AssertionError(); }
            public Project decode(String payload) { return decoder.decode(payload); }
        };
    }

    private static final class JdbcStub implements DataSource {
        final String payload;
        SQLException failure;
        String sql;
        String parameter;
        final AtomicInteger connections = new AtomicInteger();
        final AtomicBoolean connectionClosed = new AtomicBoolean();
        final AtomicBoolean statementClosed = new AtomicBoolean();
        final AtomicBoolean resultClosed = new AtomicBoolean();
        JdbcStub(String payload) { this.payload = payload; }
        public Connection getConnection() throws SQLException {
            connections.incrementAndGet();
            if (failure != null) throw failure;
            return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Connection.class},
                    (p, m, a) -> switch (m.getName()) {
                        case "prepareStatement" -> { sql = (String) a[0]; yield statement(); }
                        case "close" -> { connectionClosed.set(true); yield null; }
                        default -> defaultValue(m.getReturnType());
                    });
        }
        private PreparedStatement statement() {
            return (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{PreparedStatement.class},
                    (p, m, a) -> switch (m.getName()) {
                        case "setString" -> { parameter = (String) a[1]; yield null; }
                        case "executeQuery" -> result();
                        case "close" -> { statementClosed.set(true); yield null; }
                        default -> defaultValue(m.getReturnType());
                    });
        }
        private ResultSet result() {
            AtomicInteger next = new AtomicInteger();
            return (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ResultSet.class},
                    (p, m, a) -> switch (m.getName()) {
                        case "next" -> payload != null && next.getAndIncrement() == 0;
                        case "getString" -> payload;
                        case "close" -> { resultClosed.set(true); yield null; }
                        default -> defaultValue(m.getReturnType());
                    });
        }
        public Connection getConnection(String u, String p) throws SQLException { return getConnection(); }
        public PrintWriter getLogWriter() { return null; } public void setLogWriter(PrintWriter out) { }
        public void setLoginTimeout(int seconds) { } public int getLoginTimeout() { return 0; }
        public Logger getParentLogger() throws SQLFeatureNotSupportedException { throw new SQLFeatureNotSupportedException(); }
        public <T> T unwrap(Class<T> type) throws SQLException { throw new SQLException(); }
        public boolean isWrapperFor(Class<?> type) { return false; }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        return 0;
    }
}
