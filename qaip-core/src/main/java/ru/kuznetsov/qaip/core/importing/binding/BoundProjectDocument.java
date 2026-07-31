package ru.kuznetsov.qaip.core.importing.binding;

import ru.kuznetsov.qaip.core.domain.Project;

import java.util.Objects;

public record BoundProjectDocument(Project project) {
    public BoundProjectDocument {
        Objects.requireNonNull(project, "project");
    }
}
