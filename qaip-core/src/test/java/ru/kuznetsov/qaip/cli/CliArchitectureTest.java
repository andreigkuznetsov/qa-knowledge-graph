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
    void entry_point_exposes_exactly_one_command_and_build_uses_no_third_party_cli_framework() throws Exception {
        String application = source("QaipCliApplication.java");
        assertEquals(1, occurrences(application, "\"summary\""));
        String build = Files.readString(Path.of("build.gradle"));
        for (String forbidden : List.of("picocli", "jcommander", "commons-cli")) {
            assertFalse(build.toLowerCase(java.util.Locale.ROOT).contains(forbidden));
        }
    }

    private static String source(String name) throws Exception {
        return Files.readString(Path.of("src/main/java/ru/kuznetsov/qaip/cli").resolve(name));
    }
    private static int occurrences(String source, String token) {
        return (source.length() - source.replace(token, "").length()) / token.length();
    }
}
