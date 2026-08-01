package ru.kuznetsov.qaip.core.persistence.postgresql;

import ru.kuznetsov.qaip.core.domain.Metadata;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.persistence.ProjectAlreadyExists;
import ru.kuznetsov.qaip.core.persistence.ProjectInsertResult;
import ru.kuznetsov.qaip.core.persistence.ProjectInserted;
import ru.kuznetsov.qaip.core.persistence.ProjectPersistenceException;
import ru.kuznetsov.qaip.core.persistence.ProjectRepository;
import ru.kuznetsov.qaip.core.persistence.document.ProjectPersistenceDocumentCodec;
import ru.kuznetsov.qaip.core.persistence.document.ProjectPersistenceDocumentException;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Objects;

public final class PostgreSqlProjectRepository implements ProjectRepository {
    static final String INSERT_SQL = """
            INSERT INTO qaip_projects (project_id, project_payload)
            VALUES (?, CAST(? AS jsonb))
            ON CONFLICT (project_id) DO NOTHING
            """;

    private final DataSource dataSource;
    private final ProjectPersistenceDocumentCodec codec;

    public PostgreSqlProjectRepository(DataSource dataSource) {
        this(dataSource, ProjectPersistenceDocumentCodec.v1());
    }

    PostgreSqlProjectRepository(DataSource dataSource, ProjectPersistenceDocumentCodec codec) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    @Override
    public ProjectInsertResult insertIfAbsent(Project project) {
        Objects.requireNonNull(project, "project");
        String projectId = projectId(project);
        String payload;
        try {
            payload = codec.encode(project);
        } catch (ProjectPersistenceDocumentException exception) {
            throw new ProjectPersistenceException("Cannot encode persistence document for project '" + projectId + "'", exception);
        }

        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setString(1, projectId);
            statement.setString(2, payload);
            int updateCount = statement.executeUpdate();
            if (updateCount == 1) return new ProjectInserted(projectId);
            if (updateCount == 0) return new ProjectAlreadyExists(projectId);
            throw new ProjectPersistenceException(
                    "Unexpected insert update count for project '" + projectId + "': " + updateCount);
        } catch (SQLException exception) {
            throw new ProjectPersistenceException("Cannot insert project '" + projectId + "'", exception);
        }
    }

    private static String projectId(Project project) {
        Metadata metadata = project.metadata();
        if (metadata == null || metadata.id() == null || metadata.id().isBlank()) {
            throw new ProjectPersistenceException("Project has no usable canonical identifier");
        }
        return metadata.id();
    }
}
