package ru.kuznetsov.qaip.core.persistence.postgresql;

import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.persistence.ProjectPersistenceException;
import ru.kuznetsov.qaip.core.persistence.document.ProjectPersistenceDocumentCodec;
import ru.kuznetsov.qaip.core.persistence.document.ProjectPersistenceDocumentException;
import ru.kuznetsov.qaip.core.persistence.read.ProjectReader;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

public final class PostgreSqlProjectReader implements ProjectReader {
    static final String SELECT_SQL = "SELECT project_payload FROM qaip_projects WHERE project_id = ?";

    private final DataSource dataSource;
    private final ProjectPersistenceDocumentCodec codec;

    public PostgreSqlProjectReader(DataSource dataSource) {
        this(dataSource, ProjectPersistenceDocumentCodec.v1());
    }

    PostgreSqlProjectReader(DataSource dataSource, ProjectPersistenceDocumentCodec codec) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    @Override
    public Optional<Project> findById(String projectId) {
        Objects.requireNonNull(projectId, "projectId");
        if (projectId.isBlank()) throw new IllegalArgumentException("projectId must not be blank");

        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(SELECT_SQL)) {
            statement.setString(1, projectId);
            try (var result = statement.executeQuery()) {
                if (!result.next()) return Optional.empty();
                String payload = result.getString(1);
                Project project;
                try {
                    project = codec.decode(payload);
                } catch (ProjectPersistenceDocumentException exception) {
                    throw new ProjectPersistenceException(
                            "Cannot decode persistence document for project '" + projectId + "'", exception);
                }
                String storedId = project.metadata().id();
                if (!projectId.equals(storedId)) {
                    throw new ProjectPersistenceException("Stored project identity mismatch: requested '"
                            + projectId + "', payload contains '" + storedId + "'");
                }
                if (result.next()) {
                    throw new ProjectPersistenceException("Multiple stored projects found for identifier '"
                            + projectId + "'");
                }
                return Optional.of(project);
            }
        } catch (SQLException exception) {
            throw new ProjectPersistenceException("Cannot read project '" + projectId + "'", exception);
        }
    }
}
