package ru.kuznetsov.qaip.core.persistence.document;

import ru.kuznetsov.qaip.core.domain.Project;

interface ProjectPersistenceDocumentCodec {
    String encode(Project project);

    Project decode(String payload);
}
