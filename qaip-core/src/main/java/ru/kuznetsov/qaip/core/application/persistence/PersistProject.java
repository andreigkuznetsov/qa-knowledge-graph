package ru.kuznetsov.qaip.core.application.persistence;

import ru.kuznetsov.qaip.core.validation.ApplicationValidProjectDocument;

public interface PersistProject {
    PersistProjectResult execute(ApplicationValidProjectDocument document);
}
