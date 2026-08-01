package ru.kuznetsov.qaip.core.importing.binding;

import ru.kuznetsov.qaip.core.domain.Project;

public final class BoundProjectDocuments {
    private BoundProjectDocuments() { }

    public static BoundProjectDocument forTesting(Project project) {
        return new BoundProjectDocument(project);
    }
}
