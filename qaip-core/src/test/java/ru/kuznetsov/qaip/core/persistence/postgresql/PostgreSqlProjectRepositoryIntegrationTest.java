package ru.kuznetsov.qaip.core.persistence.postgresql;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.kuznetsov.qaip.core.application.importing.DefaultProjectImporter;
import ru.kuznetsov.qaip.core.application.importing.ProjectImportSuccess;
import ru.kuznetsov.qaip.core.application.importing.ProjectImporter;
import ru.kuznetsov.qaip.core.application.persistence.DefaultPersistProject;
import ru.kuznetsov.qaip.core.application.persistence.PersistProjectAccepted;
import ru.kuznetsov.qaip.core.application.persistence.PersistProjectFindingCode;
import ru.kuznetsov.qaip.core.application.persistence.PersistProjectRejected;
import ru.kuznetsov.qaip.core.domain.DeclaredChange;
import ru.kuznetsov.qaip.core.domain.EvidenceManifest;
import ru.kuznetsov.qaip.core.domain.Metadata;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.domain.Subject;
import ru.kuznetsov.qaip.core.importing.binding.DefaultProjectBinder;
import ru.kuznetsov.qaip.core.importing.parsing.JacksonProjectJsonParser;
import ru.kuznetsov.qaip.core.importing.parsing.NetworkntProjectSchemaValidator;
import ru.kuznetsov.qaip.core.importing.parsing.RawProjectJson;
import ru.kuznetsov.qaip.core.persistence.ProjectAlreadyExists;
import ru.kuznetsov.qaip.core.persistence.ProjectInserted;
import ru.kuznetsov.qaip.core.persistence.ProjectRepository;
import ru.kuznetsov.qaip.core.persistence.ProjectRepositoryContractTest;
import ru.kuznetsov.qaip.core.persistence.document.ProjectPersistenceDocumentCodec;
import ru.kuznetsov.qaip.core.validation.DefaultProjectApplicationValidator;

import javax.sql.DataSource;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlProjectRepositoryIntegrationTest extends ProjectRepositoryContractTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.6-alpine")
            .withStartupTimeout(Duration.ofSeconds(60));

    private static final ProjectPersistenceDocumentCodec CODEC = ProjectPersistenceDocumentCodec.v1();
    private static DataSource dataSource;

    @BeforeAll
    static void createSchema() throws Exception {
        PGSimpleDataSource configured = new PGSimpleDataSource();
        configured.setURL(POSTGRES.getJdbcUrl());
        configured.setUser(POSTGRES.getUsername());
        configured.setPassword(POSTGRES.getPassword());
        dataSource = configured;
        try (var stream = PostgreSqlProjectRepositoryIntegrationTest.class.getResourceAsStream(
                "/postgresql/qaip-projects.sql");
             var connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
            statement.execute(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @BeforeEach
    void truncate() throws Exception {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("TRUNCATE TABLE qaip_projects");
        }
    }

    @Override
    protected ProjectRepository repository() {
        return new PostgreSqlProjectRepository(dataSource);
    }

    @Override
    protected Project storedProject(ProjectRepository repository, String projectId) {
        return readProject(projectId);
    }

    @Test
    void jsonb_round_trip_preserves_all_numeric_subtypes_scale_null_and_nested_values() {
        Project original = richProject("P-FIDELITY", "Original");
        ProjectInserted inserted = assertInstanceOf(ProjectInserted.class, repository().insertIfAbsent(original));
        assertEquals(original.metadata().id(), inserted.projectId());
        assertEquals(original, readProject("P-FIDELITY"));
        assertEquals(1, rowCount("P-FIDELITY"));
    }

    @Test
    void structurally_different_duplicate_leaves_jsonb_payload_unchanged() {
        Project original = richProject("P-1", "Original");
        Project duplicate = richProject("P-1", "Different");
        ProjectRepository repository = repository();
        repository.insertIfAbsent(original);
        String payloadBefore = readPayload("P-1");

        ProjectAlreadyExists result = assertInstanceOf(ProjectAlreadyExists.class,
                repository.insertIfAbsent(duplicate));

        assertEquals("P-1", result.projectId());
        assertEquals(1, rowCount("P-1"));
        assertEquals(payloadBefore, readPayload("P-1"));
        assertEquals(original, readProject("P-1"));
    }

    @Test
    void concurrent_same_id_has_exactly_one_winner_and_stores_winning_project() throws Exception {
        int attempts = 12;
        ProjectRepository repository = repository();
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(attempts)) {
            List<Future<Attempt>> futures = new ArrayList<>();
            for (int index = 0; index < attempts; index++) {
                Project candidate = richProject("P-RACE", "Candidate-" + index);
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("start timeout");
                    return new Attempt(candidate, repository.insertIfAbsent(candidate));
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            List<Attempt> results = new ArrayList<>();
            for (Future<Attempt> future : futures) results.add(future.get(15, TimeUnit.SECONDS));
            List<Attempt> winners = results.stream().filter(value -> value.result() instanceof ProjectInserted).toList();
            assertEquals(1, winners.size());
            assertEquals(attempts - 1,
                    results.stream().filter(value -> value.result() instanceof ProjectAlreadyExists).count());
            assertEquals(winners.getFirst().project(), readProject("P-RACE"));
            assertEquals(1, rowCount("P-RACE"));
        }
    }

    @Test
    void concurrent_different_ids_do_not_interfere() throws Exception {
        int attempts = 8;
        ProjectRepository repository = repository();
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(attempts)) {
            List<Future<?>> futures = new ArrayList<>();
            for (int index = 0; index < attempts; index++) {
                Project candidate = richProject("P-" + index, "Project-" + index);
                futures.add(executor.submit(() -> {
                    if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("start timeout");
                    return repository.insertIfAbsent(candidate);
                }));
            }
            start.countDown();
            for (Future<?> future : futures) assertInstanceOf(ProjectInserted.class, future.get(15, TimeUnit.SECONDS));
            assertEquals(attempts, totalRows());
        }
    }

    @Test
    void real_import_and_application_persistence_flow_is_first_write_wins_and_lossless() throws IOException {
        ProjectImporter importer = new DefaultProjectImporter(new JacksonProjectJsonParser(),
                new NetworkntProjectSchemaValidator(), new DefaultProjectBinder(),
                new DefaultProjectApplicationValidator());
        ProjectImportSuccess imported = assertInstanceOf(ProjectImportSuccess.class,
                importer.importProject(new RawProjectJson(importJson())));
        Project exactProofProject = imported.document().project();
        DefaultPersistProject persistence = new DefaultPersistProject(repository());

        PersistProjectAccepted accepted = assertInstanceOf(PersistProjectAccepted.class,
                persistence.execute(imported.document()));
        PersistProjectRejected rejected = assertInstanceOf(PersistProjectRejected.class,
                persistence.execute(imported.document()));

        assertEquals("P-1", accepted.projectId());
        assertEquals(PersistProjectFindingCode.PROJECT_ALREADY_EXISTS, rejected.finding().code());
        assertEquals(exactProofProject, readProject("P-1"));
        assertEquals(1, rowCount("P-1"));
    }

    private static Project readProject(String projectId) {
        return CODEC.decode(readPayload(projectId));
    }

    private static String readPayload(String projectId) {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(
                     "SELECT project_payload::text FROM qaip_projects WHERE project_id = ?")) {
            statement.setString(1, projectId);
            try (var result = statement.executeQuery()) {
                if (!result.next()) return null;
                return result.getString(1);
            }
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private static int rowCount(String projectId) {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement("SELECT count(*) FROM qaip_projects WHERE project_id = ?")) {
            statement.setString(1, projectId);
            try (var result = statement.executeQuery()) { result.next(); return result.getInt(1); }
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private static int totalRows() {
        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT count(*) FROM qaip_projects")) {
            result.next();
            return result.getInt(1);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private static Project richProject(String id, String name) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("integer", Integer.valueOf(1));
        values.put("long", Long.valueOf(1));
        values.put("bigInteger", new BigInteger("1234567890123456789012345678901234567890"));
        values.put("decimal1", new BigDecimal("1"));
        values.put("decimal10", new BigDecimal("1.0"));
        values.put("decimal100", new BigDecimal("1.00"));
        values.put("precise", new BigDecimal("1234567890.123456789012345678900"));
        values.put("negativeScale", new BigDecimal(BigInteger.valueOf(123), -20));
        values.put("null", null);
        values.put("nested", List.of(Map.of("large", new BigInteger("9".repeat(1000))), List.of(), Map.of()));
        return new Project("qaip-project-v1", "0.1", new Metadata(id, name, null, null, values),
                List.of(), new Subject("local"), List.of(), List.of(), evidence(), changes(), Map.of());
    }

    private static EvidenceManifest evidence() {
        return new EvidenceManifest("impact-evidence-manifest-v1", "source", Map.of(),
                "normalization", "canonicalization", "fingerprint", List.of(), List.of(), List.of());
    }

    private static List<DeclaredChange> changes() {
        return List.of(new DeclaredChange("NODE", "N-1", "ADDED", "0.1", null, Map.of()));
    }

    private static String importJson() throws IOException {
        try (var stream = PostgreSqlProjectRepositoryIntegrationTest.class.getResourceAsStream(
                "/schema/valid/representative-project.json")) {
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return json.replace("\"project\":{\"id\":\"P-1\",\"name\":\"Project\"}",
                    "\"project\":{\"id\":\"P-1\",\"name\":\"Project\",\"metadata\":{" +
                            "\"integer\":1,\"long\":9223372036854775807," +
                            "\"big\":9223372036854775808," +
                            "\"precise\":1234567890.123456789012345678900,\"null\":null}}");
        }
    }

    private record Attempt(Project project, ru.kuznetsov.qaip.core.persistence.ProjectInsertResult result) { }
}
