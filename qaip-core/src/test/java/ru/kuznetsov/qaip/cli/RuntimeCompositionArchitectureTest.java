package ru.kuznetsov.qaip.cli;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeCompositionArchitectureTest {
    @Test
    void production_runtime_uses_postgresql_only_and_introduces_no_bootstrap_or_connectivity_check() throws Exception {
        String application = source("QaipCliApplication.java");
        String composition = source("RuntimeComposition.java");

        assertFalse(application.contains("InMemoryProject"));
        assertFalse(composition.contains("InMemoryProject"));
        assertTrue(composition.contains("RuntimeDataSourceFactory::create"));
        assertTrue(composition.contains("new PostgreSqlProjectRepository(dataSource)"));
        assertTrue(composition.contains("new PostgreSqlProjectReader(dataSource)"));
        for (String forbidden : List.of("getConnection(", "CREATE TABLE", "qaip_projects", "execute(")) {
            assertFalse(composition.contains(forbidden), forbidden);
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
