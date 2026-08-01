package ru.kuznetsov.qaip.core.application.persistence;

import ru.kuznetsov.qaip.core.persistence.ProjectAlreadyExists;
import ru.kuznetsov.qaip.core.persistence.ProjectInserted;
import ru.kuznetsov.qaip.core.persistence.ProjectRepository;
import ru.kuznetsov.qaip.core.validation.ApplicationValidProjectDocument;

import java.util.Objects;

public final class DefaultPersistProject implements PersistProject {
    private final ProjectRepository repository;

    public DefaultPersistProject(ProjectRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public PersistProjectResult execute(ApplicationValidProjectDocument document) {
        Objects.requireNonNull(document, "document");
        var result = Objects.requireNonNull(
                repository.insertIfAbsent(document.project()), "repository result");
        if (result instanceof ProjectInserted inserted) {
            return new PersistProjectAccepted(inserted.projectId());
        }
        ProjectAlreadyExists existing = (ProjectAlreadyExists) result;
        return new PersistProjectRejected(new PersistProjectFinding(
                PersistProjectFindingCode.PROJECT_ALREADY_EXISTS,
                "Project '" + existing.projectId() + "' already exists.",
                existing.projectId()));
    }
}
