package ru.kuznetsov.qaip.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class ImportCliCrossProcessSmokeTest {
    private static final String PROJECT_ID = "P-IMPORT-SMOKE-7FD29C4A";
    private static final String INVALID_PROJECT_ID = "P-IMPORT-INVALID-7FD29C4A";
    private static final String MISSING_PROJECT_ID = "P-IMPORT-MISSING-7FD29C4A";
    private static final String PASSWORD = "r2_9-credential-7fd29c4a";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.6-alpine")
            .withUsername("qaip_import_smoke_user")
            .withPassword(PASSWORD)
            .withStartupTimeout(Duration.ofSeconds(60));

    @TempDir
    Path temporaryDirectory;

    @Test
    void installed_cli_imports_and_independent_processes_query_one_durable_project() throws Exception {
        Path launcher = Path.of("build", "install", "qaip", "bin", "qaip.bat").toAbsolutePath();
        assertTrue(Files.isRegularFile(launcher), "installDist launcher is missing: " + launcher);
        Path projectFile = temporaryDirectory.resolve("project.json");
        ObjectNode project = projectDocument(PROJECT_ID, "Original rule");
        writeJson(projectFile, project);

        List<ProcessResult> results = new ArrayList<>();
        ProcessResult imported = run(launcher, "import", projectFile.toString());
        results.add(imported);
        assertResult(imported, 0, lines(
                "Import completed",
                "Project ID: " + PROJECT_ID,
                "Nodes: 5",
                "Relationships: 3"), "");

        ProcessResult summary = run(launcher, "summary", PROJECT_ID);
        ProcessResult node = run(launcher, "show", "node", PROJECT_ID, "BR-1");
        ProcessResult relationships = run(launcher, "show", "relationships", PROJECT_ID, "BR-1");
        ProcessResult trace = run(launcher, "trace", PROJECT_ID, "S-1");
        ProcessResult validation = run(launcher, "validate", "project", PROJECT_ID);
        results.addAll(List.of(summary, node, relationships, trace, validation));

        assertResult(summary, 0, lines(
                "Project Summary",
                "Project ID: " + PROJECT_ID,
                "Contract Version: qaip-project-v1",
                "Schema Version: 0.1",
                "Sources: 0",
                "Nodes: 5",
                "Relationships: 3",
                "Evidence: 4",
                "Declared Changes: 2"), "");
        assertContainsResult(node, 0, "Node ID: BR-1", "Name: Original rule");
        assertContainsResult(relationships, 0,
                "REL-2 | COVERS | S-1 -> BR-1",
                "REL-3 | DEPENDS_ON | BR-1 -> BR-2");
        assertContainsResult(trace, 0,
                "Start Node ID: S-1",
                "T-1 | TEST_IMPLEMENTATION",
                "BR-1 | BUSINESS_RULE",
                "BR-2 | BUSINESS_RULE",
                "REL-1 | VALIDATES | T-1 -> S-1",
                "REL-2 | COVERS | S-1 -> BR-1",
                "REL-3 | DEPENDS_ON | BR-1 -> BR-2");
        assertContainsResult(validation, 0,
                "Status: INVALID",
                "Errors: 1",
                "Warnings: 1",
                "[WARNING] NODE_WITHOUT_RELATIONSHIPS",
                "[ERROR] SCENARIO_WITHOUT_TEST");

        writeJson(projectFile, projectDocument(PROJECT_ID, "Replacement must not win"));
        ProcessResult duplicate = run(launcher, "import", projectFile.toString());
        ProcessResult afterDuplicate = run(launcher, "show", "node", PROJECT_ID, "BR-1");
        results.addAll(List.of(duplicate, afterDuplicate));
        assertResult(duplicate, 0, lines(
                "Import rejected",
                "Code: PROJECT_ALREADY_EXISTS",
                "Message: Project '" + PROJECT_ID + "' already exists."), "");
        assertContainsResult(afterDuplicate, 0, "Name: Original rule");
        assertTrue(!afterDuplicate.stdout().contains("Replacement must not win"));

        Path invalidFile = temporaryDirectory.resolve("invalid-project.json");
        ObjectNode invalid = projectDocument(INVALID_PROJECT_ID, "Invalid project");
        ((ObjectNode) invalid.get("baseModel")).putNull("nodes");
        writeJson(invalidFile, invalid);
        ProcessResult invalidImport = run(launcher, "import", invalidFile.toString());
        ProcessResult invalidSummary = run(launcher, "summary", INVALID_PROJECT_ID);
        results.addAll(List.of(invalidImport, invalidSummary));
        assertContainsResult(invalidImport, 0,
                "Import rejected", "Stage: SCHEMA_VALIDATION", "Findings:", "[ERROR] SCHEMA_VIOLATION");
        assertResult(invalidSummary, 3,
                lines("Project not found: " + INVALID_PROJECT_ID), "");

        Path missingFile = temporaryDirectory.resolve("missing-project.json");
        ProcessResult missingImport = run(launcher, "import", missingFile.toString());
        ProcessResult missingSummary = run(launcher, "summary", MISSING_PROJECT_ID);
        results.addAll(List.of(missingImport, missingSummary));
        assertResult(missingImport, 4, "", lines("Import failed."));
        assertResult(missingSummary, 3,
                lines("Project not found: " + MISSING_PROJECT_ID), "");

        assertEquals(results.size(), new HashSet<>(results.stream().map(ProcessResult::pid).toList()).size());
    }

    private static ObjectNode projectDocument(String projectId, String ruleName) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root;
        try (var stream = ImportCliCrossProcessSmokeTest.class.getResourceAsStream(
                "/schema/valid/representative-project.json")) {
            root = (ObjectNode) mapper.readTree(stream);
        }
        ObjectNode baseModel = (ObjectNode) root.get("baseModel");
        ((ObjectNode) baseModel.get("project")).put("id", projectId).put("name", "Import smoke project");
        baseModel.set("nodes", mapper.readTree("""
                [
                  {"id":"T-1","type":"TEST_IMPLEMENTATION","name":"Smoke test","testImplementation":{"code":"T-1","executionType":"AUTOMATED","preconditions":[],"steps":[]}},
                  {"id":"S-1","type":"SCENARIO","name":"Covered scenario","scenario":{"code":"S-1","given":[],"when":[{"id":"W-1","text":"act"}],"then":[{"id":"TH-1","text":"observe"}]}},
                  {"id":"BR-1","type":"BUSINESS_RULE","name":"%s","rule":{"code":"BR-1","ruleType":"BUSINESS_INVARIANT","text":"first"}},
                  {"id":"BR-2","type":"BUSINESS_RULE","name":"Second rule","rule":{"code":"BR-2","ruleType":"BUSINESS_INVARIANT","text":"second"}},
                  {"id":"S-2","type":"SCENARIO","name":"Uncovered scenario","scenario":{"code":"S-2","given":[],"when":[{"id":"W-2","text":"act"}],"then":[{"id":"TH-2","text":"observe"}]}}
                ]
                """.formatted(ruleName)));
        baseModel.set("relationships", mapper.readTree("""
                [
                  {"id":"REL-1","from":"T-1","type":"VALIDATES","to":"S-1"},
                  {"id":"REL-2","from":"S-1","type":"COVERS","to":"BR-1"},
                  {"id":"REL-3","from":"BR-1","type":"DEPENDS_ON","to":"BR-2"}
                ]
                """));
        return root;
    }

    private static void writeJson(Path file, ObjectNode document) throws Exception {
        Files.writeString(file, new ObjectMapper().writeValueAsString(document), StandardCharsets.UTF_8);
    }

    private static ProcessResult run(Path launcher, String... arguments) throws Exception {
        List<String> command = new ArrayList<>(List.of("cmd.exe", "/d", "/q", "/c", launcher.toString()));
        command.addAll(List.of(arguments));
        ProcessBuilder builder = new ProcessBuilder(command);
        Map<String, String> environment = builder.environment();
        environment.put("QAIP_DB_URL", POSTGRES.getJdbcUrl());
        environment.put("QAIP_DB_USER", POSTGRES.getUsername());
        environment.put("QAIP_DB_PASSWORD", POSTGRES.getPassword());
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
        assertSafe(result);
    }

    private static void assertContainsResult(ProcessResult result, int exitCode, String... fragments) {
        assertEquals(exitCode, result.exitCode(), result.stdout() + result.stderr());
        assertEquals("", result.stderr());
        for (String fragment : fragments) assertTrue(result.stdout().contains(fragment), result.stdout());
        assertSafe(result);
    }

    private static void assertSafe(ProcessResult result) {
        String output = result.stdout() + result.stderr();
        assertTrue(!output.contains("SLF4J"), output);
        assertTrue(!output.contains("Exception"), output);
        assertTrue(!output.contains("\tat "), output);
        assertTrue(!output.contains(PASSWORD), "credentials leaked");
    }

    private static String lines(String... values) {
        return String.join(System.lineSeparator(), values) + System.lineSeparator();
    }

    private record ProcessResult(long pid, int exitCode, String stdout, String stderr) { }
}
