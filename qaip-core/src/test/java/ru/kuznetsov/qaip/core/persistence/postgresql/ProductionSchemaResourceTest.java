package ru.kuznetsov.qaip.core.persistence.postgresql;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionSchemaResourceTest {
    private static final String EXPECTED = """
            CREATE TABLE IF NOT EXISTS qaip_projects (
                project_id text PRIMARY KEY,
                project_payload jsonb NOT NULL
            );
            """;

    @Test
    void production_schema_is_available_on_the_runtime_classpath_with_the_exact_contract() throws Exception {
        try (var stream = ProductionSchemaResourceTest.class.getResourceAsStream(
                "/postgresql/qaip-projects.sql")) {
            String sql = new String(Objects.requireNonNull(stream,
                    "Missing production SQL resource /postgresql/qaip-projects.sql").readAllBytes(),
                    StandardCharsets.UTF_8);
            assertEquals(EXPECTED, sql);
        }
    }

    @Test
    void schema_definition_exists_only_in_production_resources_and_contains_no_extra_sql() throws Exception {
        Path production = Path.of("src/main/resources/postgresql/qaip-projects.sql");
        Path formerTestCopy = Path.of("src/test/resources/postgresql/qaip-projects.sql");

        assertTrue(Files.isRegularFile(production));
        assertFalse(Files.exists(formerTestCopy));
        String sql = Files.readString(production);
        assertEquals(EXPECTED, sql);
        assertFalse(sql.contains("ALTER TABLE"));
        assertFalse(sql.contains("DROP TABLE"));
        assertFalse(sql.contains("CREATE INDEX"));
    }
}
