package ru.kuznetsov.qaip.core.importing.binding;

import ru.kuznetsov.qaip.core.domain.Project;

import java.util.Objects;

public final class BoundProjectDocument {
    private final Project project;

    BoundProjectDocument(Project project) {
        this.project = Objects.requireNonNull(project, "project");
    }

    public Project project() {
        return project;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof BoundProjectDocument that && project.equals(that.project);
    }

    @Override
    public int hashCode() {
        return project.hashCode();
    }

    @Override
    public String toString() {
        return "BoundProjectDocument[project=" + project + ']';
    }
}
