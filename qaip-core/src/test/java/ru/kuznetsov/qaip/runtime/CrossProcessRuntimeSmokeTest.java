package ru.kuznetsov.qaip.runtime;

import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.kuznetsov.qaip.core.domain.DeclaredChange;
import ru.kuznetsov.qaip.core.domain.EvidenceManifest;
import ru.kuznetsov.qaip.core.domain.Metadata;
import ru.kuznetsov.qaip.core.domain.Node;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.domain.Relationship;
import ru.kuznetsov.qaip.core.domain.Subject;
import ru.kuznetsov.qaip.core.persistence.ProjectInserted;
import ru.kuznetsov.qaip.core.persistence.postgresql.PostgreSqlProjectRepository;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class CrossProcessRuntimeSmokeTest {
    private static final String PROJECT_ID = "P-RUNTIME-SMOKE";
    private static final String NODE_ID = "S-1";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.6-alpine")
            .withUsername("qaip_smoke_user")
            .withPassword("r1_7-credential-7fd29c4a")
            .withStartupTimeout(Duration.ofSeconds(60));

    @Test
    void persisted_project_is_available_to_every_installed_cli_command_in_independent_processes() throws Exception {
        DataSource dataSource = dataSource();
        PostgreSqlSchemaBootstrap bootstrap = new PostgreSqlSchemaBootstrap();
        bootstrap.initialize(dataSource);
        assertInstanceOf(ProjectInserted.class,
                new PostgreSqlProjectRepository(dataSource).insertIfAbsent(project()));
        bootstrap.initialize(dataSource);

        Path launcher = Path.of("build", "install", "qaip", "bin", "qaip.bat").toAbsolutePath();
        assertTrue(Files.isRegularFile(launcher), "installDist launcher is missing: " + launcher);

        List<ProcessResult> results = new ArrayList<>();
        results.add(run(launcher, true, "summary", PROJECT_ID));
        results.add(run(launcher, true, "show", "node", PROJECT_ID, NODE_ID));
        results.add(run(launcher, true, "show", "relationships", PROJECT_ID, NODE_ID));
        results.add(run(launcher, true, "trace", PROJECT_ID, NODE_ID));
        results.add(run(launcher, true, "validate", "project", PROJECT_ID));
        results.add(run(launcher, true, "summary", "P-MISSING"));
        results.add(run(launcher, false, "unknown"));

        assertResult(results.get(0), 0, lines(
                "Project Summary",
                "Project ID: " + PROJECT_ID,
                "Contract Version: qaip-project-v1",
                "Schema Version: 0.1",
                "Sources: 0",
                "Nodes: 5",
                "Relationships: 3",
                "Evidence: 0",
                "Declared Changes: 1"), "");
        assertResult(results.get(1), 0, lines(
                "Node Details",
                "Project ID: " + PROJECT_ID,
                "Node ID: S-1",
                "Type: SCENARIO",
                "Name: Checkout scenario",
                "Description: transitive smoke fixture",
                "Status: READY"), "");
        assertResult(results.get(2), 0, lines(
                "Relationships",
                "Project ID: " + PROJECT_ID,
                "Node ID: S-1",
                "",
                "Incoming:",
                "REL-1 | VALIDATES | T-1 -> S-1",
                "",
                "Outgoing:",
                "REL-2 | TRACES_TO | S-1 -> R-1"), "");
        assertResult(results.get(3), 0, lines(
                "Trace",
                "Project ID: " + PROJECT_ID,
                "Start Node ID: S-1",
                "",
                "Nodes:",
                "S-1 | SCENARIO",
                "T-1 | TEST_IMPLEMENTATION",
                "R-1 | REQUIREMENT",
                "S-2 | SCENARIO",
                "",
                "Relationships:",
                "REL-1 | VALIDATES | T-1 -> S-1",
                "REL-2 | TRACES_TO | S-1 -> R-1",
                "REL-3 | IMPACTS | R-1 -> S-2"), "");
        assertResult(results.get(4), 0, lines(
                "Validation Report",
                "Project ID: " + PROJECT_ID,
                "Status: INVALID",
                "Errors: 1",
                "Warnings: 1",
                "",
                "Issues:",
                "[WARNING] NODE_WITHOUT_RELATIONSHIPS | rule=ISOLATED_NODE | Node 'I-1' has no relationships. | node=I-1",
                "[ERROR] SCENARIO_WITHOUT_TEST | rule=SCENARIO_REQUIRES_TEST | Scenario 'S-2' has no validating test implementation. | node=S-2"), "");
        assertResult(results.get(5), 3, lines("Project not found: P-MISSING"), "");
        assertResult(results.get(6), 2, "", lines(
                "Usage:",
                "  qaip summary <project-id>",
                "  qaip show node <project-id> <node-id>",
                "  qaip show relationships <project-id> <node-id>",
                "  qaip trace <project-id> <start-node-id>",
                "  qaip validate project <project-id>",
                "  qaip import <file>"));

        assertEquals(results.size(), new HashSet<>(results.stream().map(ProcessResult::pid).toList()).size());
    }

    private static ProcessResult run(Path launcher, boolean configureDatabase, String... arguments) throws Exception {
        List<String> command = new ArrayList<>(List.of("cmd.exe", "/d", "/q", "/c", launcher.toString()));
        command.addAll(List.of(arguments));
        ProcessBuilder builder = new ProcessBuilder(command);
        Map<String, String> environment = builder.environment();
        if (configureDatabase) {
            environment.put("QAIP_DB_URL", POSTGRES.getJdbcUrl());
            environment.put("QAIP_DB_USER", POSTGRES.getUsername());
            environment.put("QAIP_DB_PASSWORD", POSTGRES.getPassword());
        } else {
            environment.remove("QAIP_DB_URL");
            environment.remove("QAIP_DB_USER");
            environment.remove("QAIP_DB_PASSWORD");
        }
        environment.put("JAVA_OPTS", "-Dslf4j.internal.verbosity=ERROR");
        Process process = builder.start();
        assertTrue(process.waitFor(30, TimeUnit.SECONDS), "CLI process timed out: " + command);
        return new ProcessResult(process.pid(), process.exitValue(),
                new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8),
                new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    private static void assertResult(ProcessResult result, int exitCode, String stdout, String stderr) {
        assertEquals(exitCode, result.exitCode());
        assertEquals(stdout, result.stdout());
        assertEquals(stderr, result.stderr());
        String output = result.stdout() + result.stderr();
        assertTrue(!output.contains("Exception"), output);
        assertTrue(!output.contains("\tat "), output);
        assertTrue(!output.contains(POSTGRES.getPassword()), "credentials leaked");
    }

    private static String lines(String... values) {
        return String.join(System.lineSeparator(), values) + System.lineSeparator();
    }

    private static DataSource dataSource() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        return dataSource;
    }

    private static Project project() {
        List<Node> nodes = List.of(
                node("T-1", "TEST_IMPLEMENTATION", "Checkout test"),
                new Node("S-1", "SCENARIO", "Checkout scenario", "transitive smoke fixture", "READY",
                        List.of(), List.of(), Map.of(), Map.of()),
                node("R-1", "REQUIREMENT", "Checkout requirement"),
                node("S-2", "SCENARIO", "Uncovered scenario"),
                node("I-1", "RISK", "Isolated risk"));
        List<Relationship> relationships = List.of(
                relationship("REL-1", "T-1", "VALIDATES", "S-1"),
                relationship("REL-2", "S-1", "TRACES_TO", "R-1"),
                relationship("REL-3", "R-1", "IMPACTS", "S-2"));
        return new Project("qaip-project-v1", "0.1",
                new Metadata(PROJECT_ID, "Runtime smoke project", null, null, Map.of()),
                List.of(), new Subject("local"), nodes, relationships,
                new EvidenceManifest("impact-evidence-manifest-v1", "source", Map.of(),
                        "normalization", "canonicalization", "fingerprint", List.of(), List.of(), List.of()),
                List.of(new DeclaredChange("NODE", "S-1", "ADDED", "0.1", null, Map.of())), Map.of());
    }

    private static Node node(String id, String type, String name) {
        return new Node(id, type, name, null, null, List.of(), List.of(), Map.of(), Map.of());
    }

    private static Relationship relationship(String id, String from, String type, String to) {
        return new Relationship(id, from, type, to, Map.of(), List.of());
    }

    private record ProcessResult(long pid, int exitCode, String stdout, String stderr) { }
}
