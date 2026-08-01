package ru.kuznetsov.qaip.core.persistence.memory;

import ru.kuznetsov.qaip.core.domain.Metadata;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.persistence.ProjectAlreadyExists;
import ru.kuznetsov.qaip.core.persistence.ProjectInsertResult;
import ru.kuznetsov.qaip.core.persistence.ProjectInserted;
import ru.kuznetsov.qaip.core.persistence.ProjectPersistenceException;
import ru.kuznetsov.qaip.core.persistence.ProjectRepository;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryProjectRepository implements ProjectRepository {
    private final ConcurrentMap<String, Project> projects = new ConcurrentHashMap<>();

    @Override
    public ProjectInsertResult insertIfAbsent(Project project) {
        Objects.requireNonNull(project, "project");
        String projectId = projectId(project);
        Project existing = projects.putIfAbsent(projectId, project);
        return existing == null ? new ProjectInserted(projectId) : new ProjectAlreadyExists(projectId);
    }

    Project storedProject(String projectId) {
        return projects.get(projectId);
    }

    private static String projectId(Project project) {
        Metadata metadata = project.metadata();
        if (metadata == null || metadata.id() == null || metadata.id().isBlank()) {
            throw new ProjectPersistenceException("Project has no usable canonical identifier");
        }
        return metadata.id();
    }
}
