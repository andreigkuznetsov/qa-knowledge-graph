package ru.kuznetsov.qaip.core.persistence.memory;

import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.persistence.read.ProjectReader;

import java.util.Objects;
import java.util.Optional;

public final class InMemoryProjectReader implements ProjectReader {
    private final InMemoryProjectRepository repository;

    public InMemoryProjectReader(InMemoryProjectRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public Optional<Project> findById(String projectId) {
        Objects.requireNonNull(projectId, "projectId");
        if (projectId.isBlank()) throw new IllegalArgumentException("projectId must not be blank");
        return Optional.ofNullable(repository.storedProject(projectId));
    }
}
