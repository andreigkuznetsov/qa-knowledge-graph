package ru.kuznetsov.qaip.runtime;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Function;

public final class PostgreSqlSchemaBootstrap {
    private static final String RESOURCE_PATH = "postgresql/qaip-projects.sql";

    public void initialize(DataSource dataSource) {
        initialize(dataSource, path -> PostgreSqlSchemaBootstrap.class.getClassLoader().getResourceAsStream(path));
    }

    void initialize(DataSource dataSource, Function<String, InputStream> resourceLoader) {
        Objects.requireNonNull(dataSource, "dataSource");
        Objects.requireNonNull(resourceLoader, "resourceLoader");
        String sql = loadSql(resourceLoader);

        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot initialize PostgreSQL schema", exception);
        }
    }

    private static String loadSql(Function<String, InputStream> resourceLoader) {
        InputStream resource = resourceLoader.apply(RESOURCE_PATH);
        if (resource == null) {
            throw new IllegalStateException("Missing SQL resource " + RESOURCE_PATH);
        }
        try (resource) {
            return new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read SQL resource " + RESOURCE_PATH, exception);
        }
    }
}
