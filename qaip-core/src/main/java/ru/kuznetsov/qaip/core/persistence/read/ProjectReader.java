package ru.kuznetsov.qaip.core.persistence.read;

import ru.kuznetsov.qaip.core.domain.Project;

import java.util.Optional;

/** Read-only persistence boundary for complete immutable projects. */
public interface ProjectReader {
    Optional<Project> findById(String projectId);
}
