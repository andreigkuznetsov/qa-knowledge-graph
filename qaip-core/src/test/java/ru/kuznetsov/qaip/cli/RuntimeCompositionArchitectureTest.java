package ru.kuznetsov.qaip.cli;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeCompositionArchitectureTest {
    @Test
    void production_runtime_bootstraps_before_creating_postgresql_adapters_without_extra_infrastructure() throws Exception {
        String application = source("QaipCliApplication.java");
        String composition = source("RuntimeComposition.java");

        assertFalse(application.contains("InMemoryProject"));
        assertFalse(composition.contains("InMemoryProject"));
        assertTrue(composition.contains("RuntimeDataSourceFactory::create"));
        assertTrue(composition.contains("bootstrap::initialize"));
        assertTrue(composition.contains("new PostgreSqlProjectRepository(dataSource)"));
        assertTrue(composition.contains("new PostgreSqlProjectReader(dataSource)"));
        assertTrue(composition.indexOf("bootstrap).accept(dataSource)")
                < composition.indexOf("new PostgreSqlProjectRepository(dataSource)"));
        assertTrue(composition.indexOf("bootstrap).accept(dataSource)")
                < composition.indexOf("new PostgreSqlProjectReader(dataSource)"));
        for (String forbidden : List.of("getConnection(", "CREATE TABLE", "qaip_projects", "execute(")) {
            assertFalse(composition.contains(forbidden), forbidden);
        }
    }

    @Test
    void only_runtime_composition_depends_on_the_bootstrap_component() throws Exception {
        try (var files = Files.walk(Path.of("src/main/java"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                if (file.endsWith("RuntimeComposition.java") || file.endsWith("PostgreSqlSchemaBootstrap.java")) {
                    continue;
                }
                assertFalse(Files.readString(file).contains("PostgreSqlSchemaBootstrap"), file.toString());
            }
        }
    }

    @Test
    void cli_commands_remain_independent_of_runtime_and_persistence_composition() throws Exception {
        for (String command : List.of("ProjectSummaryCliCommand.java", "NodeDetailsCliCommand.java",
                "RelationshipsCliCommand.java", "TraceCliCommand.java", "ValidationCliCommand.java")) {
            String source = source(command);
            for (String forbidden : List.of("DataSource", "java.sql", "javax.sql", "ProjectRepository",
                    "ProjectReader", "PostgreSql", "RuntimeDataSourceFactory")) {
                assertFalse(source.contains(forbidden), command + " contains " + forbidden);
            }
        }
    }

    private static String source(String file) throws Exception {
        return Files.readString(Path.of("src/main/java/ru/kuznetsov/qaip/cli", file));
    }
}
