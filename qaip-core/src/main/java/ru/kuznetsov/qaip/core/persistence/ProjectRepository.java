package ru.kuznetsov.qaip.core.persistence;

import ru.kuznetsov.qaip.core.domain.Project;

public interface ProjectRepository {
    ProjectInsertResult insertIfAbsent(Project project);
}
