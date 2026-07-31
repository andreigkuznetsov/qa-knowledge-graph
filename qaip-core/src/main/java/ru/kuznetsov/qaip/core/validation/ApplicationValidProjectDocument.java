package ru.kuznetsov.qaip.core.validation;

import ru.kuznetsov.qaip.core.domain.Project;

import java.util.Objects;

public final class ApplicationValidProjectDocument {
    private final Project project;

    ApplicationValidProjectDocument(Project project) {
        this.project = Objects.requireNonNull(project, "project");
    }

    public Project project() {
        return project;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof ApplicationValidProjectDocument that
                && project.equals(that.project);
    }

    @Override
    public int hashCode() {
        return project.hashCode();
    }

    @Override
    public String toString() {
        return "ApplicationValidProjectDocument[project=" + project + ']';
    }
}
