package ru.kuznetsov.qaip.cli;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryUseCase;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CliArchitectureTest {
    @Test
    void command_depends_only_on_use_case_results_renderer_and_standard_output() throws Exception {
        String source = source("ProjectSummaryCliCommand.java");
        assertTrue(source.contains(ProjectSummaryUseCase.class.getSimpleName()));
        for (String forbidden : List.of("ProjectReader", "ProjectRepository", "InMemoryProject", "PostgreSql",
                "core.domain", "persistence.document", "java.sql", "javax.sql", "com.fasterxml", "Spring",
                "picocli", "jcommander", "commons.cli", ".nodes()", ".relationships()", ".sources()")) {
            assertFalse(source.contains(forbidden), forbidden);
        }
    }

    @Test
    void renderer_uses_only_summary_value_and_contains_no_calculation_or_infrastructure() throws Exception {
        String source = source("ProjectSummaryTextRenderer.java");
        for (String forbidden : List.of("ProjectReader", "ProjectRepository", "core.domain", "persistence",
                "java.sql", "javax.sql", "com.fasterxml", ".size()", "stream()")) {
            assertFalse(source.contains(forbidden), forbidden);
        }
    }

    @Test
    void node_command_and_renderer_depend_only_on_node_details_application_values() throws Exception {
        String command = source("NodeDetailsCliCommand.java");
        assertFalse(command.contains("String[]"));
        assertTrue(command.contains("NodeDetailsUseCase"));
        for (String forbidden : List.of("ProjectReader", "ProjectNodeLookup", "NodeDetailsMapper",
                "ProjectRepository", "core.domain", "persistence.memory", "persistence.postgresql",
                "persistence.document", "java.sql", "javax.sql", "com.fasterxml")) {
            assertFalse(command.contains(forbidden), forbidden);
        }
        String renderer = source("NodeDetailsTextRenderer.java");
        for (String forbidden : List.of("UseCase", "ProjectReader", "ProjectNodeLookup", "NodeDetailsMapper",
                "core.domain", "persistence", ".attributes()", ".tags()", ".sourceReferences()")) {
            assertFalse(renderer.contains(forbidden), forbidden);
        }
    }

    @Test
    void entry_point_exposes_exactly_four_commands_and_build_uses_no_third_party_cli_framework() throws Exception {
        String application = source("QaipCliApplication.java");
        assertTrue(application.contains("\"summary\".equals(args[0])"));
        assertTrue(application.contains("\"show\".equals(args[0])"));
        assertTrue(application.contains("\"node\".equals(args[1])"));
        assertTrue(application.contains("\"relationships\".equals(args[1])"));
        assertTrue(application.contains("\"trace\".equals(args[0])"));
        String build = Files.readString(Path.of("build.gradle"));
        for (String forbidden : List.of("picocli", "jcommander", "commons-cli")) {
            assertFalse(build.toLowerCase(java.util.Locale.ROOT).contains(forbidden));
        }
    }

    @Test
    void trace_command_and_renderer_are_application_only_and_do_not_parse_or_traverse() throws Exception {
        String command = source("TraceCliCommand.java");
        assertFalse(command.contains("String[]"));
        assertTrue(command.contains("TraceUseCase"));
        for (String forbidden : List.of("ProjectReader", "ProjectNodeLookup", "TraceGraphBuilder", "TraceMapper",
                "ProjectRepository", "core.domain", "persistence.memory", "persistence.postgresql",
                ".nodes()", ".relationships()")) {
            assertFalse(command.contains(forbidden), forbidden);
        }
        String renderer = source("TraceTextRenderer.java");
        for (String forbidden : List.of("UseCase", "ProjectReader", "TraceGraphBuilder", "TraceMapper",
                "core.domain", "persistence", "queue", "visited", "sort(", "filter(")) {
            assertFalse(renderer.contains(forbidden), forbidden);
        }
    }

    @Test
    void relationships_command_and_renderer_stay_thin_and_application_value_only() throws Exception {
        String command = source("RelationshipsCliCommand.java");
        assertFalse(command.contains("String[]"));
        assertTrue(command.contains("RelationshipsUseCase"));
        for (String forbidden : List.of("ProjectReader", "ProjectNodeLookup", "ProjectRelationshipLookup",
                "RelationshipDetailsMapper", "ProjectRepository", "core.domain", "persistence.memory",
                "persistence.postgresql", "persistence.document", ".from()", ".to()")) {
            assertFalse(command.contains(forbidden), forbidden);
        }
        String renderer = source("RelationshipsTextRenderer.java");
        for (String forbidden : List.of("UseCase", "ProjectReader", "ProjectNodeLookup",
                "ProjectRelationshipLookup", "RelationshipDetailsMapper", "core.domain", "persistence",
                ".from()", ".to()")) {
            assertFalse(renderer.contains(forbidden), forbidden);
        }
    }

    private static String source(String name) throws Exception {
        return Files.readString(Path.of("src/main/java/ru/kuznetsov/qaip/cli").resolve(name));
    }
    private static int occurrences(String source, String token) {
        return (source.length() - source.replace(token, "").length()) / token.length();
    }
}
